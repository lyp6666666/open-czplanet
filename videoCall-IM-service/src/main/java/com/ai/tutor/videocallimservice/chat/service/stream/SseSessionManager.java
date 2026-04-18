package com.ai.tutor.videocallimservice.chat.service.stream;

import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamMessageEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatPresenceResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamDeliveryEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamReadEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamTypingEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventEnvelope;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeStreamReadyResp;
import com.ai.tutor.videocallimservice.chat.service.realtime.RealtimeEventStoreService;
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SseSessionManager {

    private static final int REPLAY_BUFFER_LIMIT = 200;

    private final Map<Long, List<SessionHolder>> emittersByUid = new ConcurrentHashMap<>();
    private final Map<Long, Deque<RealtimeEventEnvelope>> replayBufferByUid = new ConcurrentHashMap<>();
    private final Map<Long, Date> lastOfflineAtByUid = new ConcurrentHashMap<>();
    private final AtomicLong eventIdGenerator = new AtomicLong(System.currentTimeMillis());
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "im-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    @Resource
    private RealtimeEventStoreService realtimeEventStoreService;

    @PostConstruct
    public void startHeartbeatTask() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeatToV2Clients, 20, 20, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopHeartbeatTask() {
        heartbeatExecutor.shutdownNow();
    }

    public SseEmitter connect(Long uid) {
        SseEmitter emitter = new SseEmitter(0L);
        SessionHolder holder = new SessionHolder(uid, null, Protocol.LEGACY, emitter);
        emittersByUid.computeIfAbsent(uid, k -> new CopyOnWriteArrayList<>()).add(holder);

        emitter.onCompletion(() -> remove(uid, holder));
        emitter.onTimeout(() -> remove(uid, holder));
        emitter.onError(e -> remove(uid, holder));

        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException e) {
            remove(uid, holder);
        }
        return emitter;
    }

    public SseEmitter connectV2(Long uid, String clientId, Long lastEventId) {
        String normalizedClientId = normalizeClientId(clientId);
        SseEmitter emitter = new SseEmitter(0L);
        SessionHolder holder = new SessionHolder(uid, normalizedClientId, Protocol.V2, emitter);
        emittersByUid.computeIfAbsent(uid, k -> new CopyOnWriteArrayList<>()).add(holder);

        emitter.onCompletion(() -> remove(uid, holder));
        emitter.onTimeout(() -> remove(uid, holder));
        emitter.onError(e -> remove(uid, holder));

        List<RealtimeEventEnvelope> replayEvents = listReplayEventsAfter(uid, lastEventId);
        try {
            emitter.send(SseEmitter.event().name("ready").data(RealtimeStreamReadyResp.builder()
                    .clientId(normalizedClientId)
                    .lastEventId(getLatestEventId(uid))
                    .replayedCount(replayEvents.size())
                    .build()));
            for (RealtimeEventEnvelope replayEvent : replayEvents) {
                emitter.send(SseEmitter.event().name("event").data(replayEvent));
            }
        } catch (IOException e) {
            remove(uid, holder);
        }
        return emitter;
    }

    public List<ChatPresenceResp> listPresence(Collection<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }
        List<ChatPresenceResp> result = new ArrayList<>();
        // 批量查询时去重并保留入参顺序，避免前端同一批请求出现重复项。
        for (Long uid : new LinkedHashSet<>(uids)) {
            if (uid == null || uid <= 0) {
                continue;
            }
            result.add(ChatPresenceResp.builder()
                    .uid(uid)
                    .online(isOnline(uid))
                    .lastOnlineAt(lastOfflineAtByUid.get(uid))
                    .build());
        }
        return result;
    }

    public boolean isOnline(Long uid) {
        return getActiveSessionCount(uid) > 0;
    }

    public void sendToUid(Long uid, String eventName, Object data) {
        RealtimeEventEnvelope envelope = buildEnvelope(uid, eventName, data);
        if (realtimeEventStoreService != null) {
            // 先落库再在线推送，这样客户端断线重连后才能按 eventId 做补偿同步。
            envelope = realtimeEventStoreService.save(envelope);
        }
        envelope = appendReplayEvent(uid, envelope);
        List<SessionHolder> emitters = emittersByUid.get(uid);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SessionHolder holder : emitters) {
            try {
                if (holder.protocol == Protocol.V2) {
                    holder.emitter.send(SseEmitter.event().name("event").data(envelope));
                } else {
                    holder.emitter.send(SseEmitter.event().name(eventName).data(data));
                }
            } catch (IOException e) {
                remove(uid, holder);
            }
        }
    }

    public void sendEphemeralToUid(Long uid, String eventName, Object data) {
        RealtimeEventEnvelope envelope = buildEphemeralEnvelope(uid, eventName, data);
        List<SessionHolder> emitters = emittersByUid.get(uid);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SessionHolder holder : emitters) {
            try {
                if (holder.protocol == Protocol.V2) {
                    holder.emitter.send(SseEmitter.event().name("event").data(envelope));
                } else {
                    holder.emitter.send(SseEmitter.event().name(eventName).data(data));
                }
            } catch (IOException e) {
                remove(uid, holder);
            }
        }
    }

    List<RealtimeEventEnvelope> listReplayEventsAfter(Long uid, Long lastEventId) {
        Deque<RealtimeEventEnvelope> replayEvents = replayBufferByUid.get(uid);
        if (replayEvents == null || replayEvents.isEmpty()) {
            return List.of();
        }
        long watermark = lastEventId == null ? 0L : lastEventId;
        List<RealtimeEventEnvelope> result = new ArrayList<>();
        for (RealtimeEventEnvelope replayEvent : replayEvents) {
            if (replayEvent != null && replayEvent.getEventId() != null && replayEvent.getEventId() > watermark) {
                result.add(replayEvent);
            }
        }
        return result;
    }

    Long getLatestEventId(Long uid) {
        // ready 阶段优先暴露“服务端已知最新水位”，让前端判断是否需要补偿拉取。
        long memoryLatest = getLatestReplayEventId(uid);
        long storedLatest = realtimeEventStoreService == null ? 0L : realtimeEventStoreService.getLatestEventId(uid);
        return Math.max(memoryLatest, storedLatest);
    }

    void sendHeartbeatToV2Clients() {
        for (Map.Entry<Long, List<SessionHolder>> entry : emittersByUid.entrySet()) {
            Long uid = entry.getKey();
            for (SessionHolder holder : entry.getValue()) {
                if (holder.protocol != Protocol.V2) {
                    continue;
                }
                try {
                    // 心跳事件不进入补偿窗口，只用于让浏览器和代理层保持活跃。
                    holder.emitter.send(SseEmitter.event().name("heartbeat").data(Map.of(
                            "uid", uid,
                            "ts", System.currentTimeMillis()
                    )));
                } catch (IOException e) {
                    remove(uid, holder);
                }
            }
        }
    }

    private RealtimeEventEnvelope appendReplayEvent(Long uid, RealtimeEventEnvelope envelope) {
        Deque<RealtimeEventEnvelope> replayEvents = replayBufferByUid.computeIfAbsent(uid, k -> new ConcurrentLinkedDeque<>());
        replayEvents.addLast(envelope);
        while (replayEvents.size() > REPLAY_BUFFER_LIMIT) {
            replayEvents.pollFirst();
        }
        return envelope;
    }

    private long getLatestReplayEventId(Long uid) {
        Deque<RealtimeEventEnvelope> replayEvents = replayBufferByUid.get(uid);
        if (replayEvents == null || replayEvents.isEmpty()) {
            return 0L;
        }
        RealtimeEventEnvelope latest = replayEvents.peekLast();
        return latest == null || latest.getEventId() == null ? 0L : latest.getEventId();
    }

    private RealtimeEventEnvelope buildEnvelope(Long uid, String eventName, Object data) {
        return buildEnvelope(uid, eventName, data, true);
    }

    private RealtimeEventEnvelope buildEphemeralEnvelope(Long uid, String eventName, Object data) {
        return buildEnvelope(uid, eventName, data, false);
    }

    private RealtimeEventEnvelope buildEnvelope(Long uid, String eventName, Object data, boolean includeEventId) {
        String normalizedEventName = eventName == null ? "" : eventName.trim();
        String eventType = "legacy." + normalizedEventName;
        String bizType = normalizedEventName.isEmpty() ? "unknown" : normalizedEventName;
        Long roomId = null;
        Long msgId = null;

        if ("message".equals(normalizedEventName) && data instanceof ChatStreamMessageEvent) {
            ChatStreamMessageEvent messageEvent = (ChatStreamMessageEvent) data;
            eventType = "chat.message.created";
            bizType = "chat";
            roomId = messageEvent.getRoomId();
            msgId = messageEvent.getMsgId();
        } else if ("delivery".equals(normalizedEventName) && data instanceof ChatStreamDeliveryEvent) {
            ChatStreamDeliveryEvent deliveryEvent = (ChatStreamDeliveryEvent) data;
            eventType = "chat.delivery.updated";
            bizType = "chat";
            roomId = deliveryEvent.getRoomId();
            msgId = deliveryEvent.getLastDeliveredMsgId();
        } else if ("read".equals(normalizedEventName) && data instanceof ChatStreamReadEvent) {
            ChatStreamReadEvent readEvent = (ChatStreamReadEvent) data;
            eventType = "chat.read.updated";
            bizType = "chat";
            roomId = readEvent.getRoomId();
            msgId = readEvent.getLastReadMsgId();
        } else if ("typing".equals(normalizedEventName) && data instanceof ChatStreamTypingEvent) {
            ChatStreamTypingEvent typingEvent = (ChatStreamTypingEvent) data;
            eventType = "chat.typing.updated";
            bizType = "chat";
            roomId = typingEvent.getRoomId();
        } else if ("application".equals(normalizedEventName)) {
            bizType = "application";
            eventType = resolveApplicationEventType(data);
        }

        return RealtimeEventEnvelope.builder()
                .eventId(includeEventId ? eventIdGenerator.incrementAndGet() : null)
                .eventType(eventType)
                .bizType(bizType)
                .targetUid(uid)
                .roomId(roomId)
                .msgId(msgId)
                .occurredAt(new Date())
                .payload(data)
                .build();
    }

    private String resolveApplicationEventType(Object data) {
        if (data instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) data;
            Object rawType = map.get("type");
            String type = rawType == null ? "" : String.valueOf(rawType).trim().toUpperCase();
            if ("CREATED".equals(type)) {
                return "application.created";
            }
            if ("DECIDED".equals(type)) {
                return "application.decided";
            }
            if ("CHAT_ENABLED".equals(type)) {
                return "application.chat_enabled";
            }
        }
        return "application.updated";
    }

    private String normalizeClientId(String clientId) {
        String normalized = clientId == null ? "" : clientId.trim();
        return normalized.isEmpty() ? "web-" + UUID.randomUUID() : normalized;
    }

    private void remove(Long uid, SessionHolder holder) {
        List<SessionHolder> emitters = emittersByUid.get(uid);
        if (emitters == null) {
            return;
        }
        emitters.remove(holder);
        if (emitters.isEmpty()) {
            emittersByUid.remove(uid);
            // 只有最后一个 SSE 会话断开时，才更新“最后在线时间”。
            lastOfflineAtByUid.put(uid, new Date());
        }
    }

    private int getActiveSessionCount(Long uid) {
        if (uid == null || uid <= 0) {
            return 0;
        }
        List<SessionHolder> emitters = emittersByUid.get(uid);
        return emitters == null ? 0 : emitters.size();
    }

    private enum Protocol {
        LEGACY,
        V2
    }

    private static final class SessionHolder {
        private final Long uid;
        private final String clientId;
        private final Protocol protocol;
        private final SseEmitter emitter;

        private SessionHolder(Long uid, String clientId, Protocol protocol, SseEmitter emitter) {
            this.uid = uid;
            this.clientId = clientId;
            this.protocol = Objects.requireNonNull(protocol);
            this.emitter = Objects.requireNonNull(emitter);
        }
    }
}
