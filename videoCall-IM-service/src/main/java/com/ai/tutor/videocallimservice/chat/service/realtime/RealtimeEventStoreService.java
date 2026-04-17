package com.ai.tutor.videocallimservice.chat.service.realtime;

import com.ai.tutor.videocallimservice.chat.domain.entity.ChatRealtimeEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventEnvelope;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventSyncResp;
import com.ai.tutor.videocallimservice.chat.mapper.ChatRealtimeEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class RealtimeEventStoreService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 200;

    @Resource
    private ChatRealtimeEventMapper chatRealtimeEventMapper;

    @Resource
    private ObjectMapper objectMapper;

    public RealtimeEventEnvelope save(RealtimeEventEnvelope envelope) {
        if (envelope == null || envelope.getTargetUid() == null || envelope.getEventId() == null) {
            return envelope;
        }
        try {
            chatRealtimeEventMapper.insertEvent(ChatRealtimeEvent.builder()
                    .eventId(envelope.getEventId())
                    .targetUid(envelope.getTargetUid())
                    .eventType(envelope.getEventType())
                    .bizType(envelope.getBizType())
                    .roomId(envelope.getRoomId())
                    .msgId(envelope.getMsgId())
                    .occurredAt(toLocalDateTime(envelope.getOccurredAt()))
                    .payloadJson(writePayload(envelope.getPayload()))
                    .build());
        } catch (DuplicateKeyException e) {
            log.debug("chat_realtime_event duplicated eventId={}", envelope.getEventId());
        } catch (Exception e) {
            // 落库失败时不能影响在线推送，否则会把现有消息链路一并打断。
            log.warn("chat_realtime_event_save_failed eventId={} targetUid={}", envelope.getEventId(), envelope.getTargetUid(), e);
        }
        return envelope;
    }

    public RealtimeEventSyncResp sync(Long targetUid, Long lastEventId, Integer pageSize) {
        if (targetUid == null || targetUid <= 0) {
            return RealtimeEventSyncResp.empty(normalizeWatermark(lastEventId), 0L);
        }
        long watermark = normalizeWatermark(lastEventId);
        int limit = normalizePageSize(pageSize) + 1;
        // 多查 1 条来判断是否还有下一页，避免前端断线补偿时漏拉。
        List<ChatRealtimeEvent> rows = chatRealtimeEventMapper.listAfter(targetUid, watermark, limit);
        Long latestEventId = normalizeLatestEventId(chatRealtimeEventMapper.selectLatestEventId(targetUid));
        if (rows == null || rows.isEmpty()) {
            return RealtimeEventSyncResp.empty(watermark, latestEventId);
        }

        boolean hasMore = rows.size() > normalizePageSize(pageSize);
        List<ChatRealtimeEvent> pageRows = hasMore ? rows.subList(0, normalizePageSize(pageSize)) : rows;
        List<RealtimeEventEnvelope> list = pageRows.stream().map(this::toEnvelope).toList();
        Long cursor = list.isEmpty() ? watermark : list.get(list.size() - 1).getEventId();
        return RealtimeEventSyncResp.builder()
                .cursor(cursor)
                .isLast(!hasMore)
                .latestEventId(latestEventId)
                .list(list)
                .build();
    }

    public Long getLatestEventId(Long targetUid) {
        if (targetUid == null || targetUid <= 0) {
            return 0L;
        }
        return normalizeLatestEventId(chatRealtimeEventMapper.selectLatestEventId(targetUid));
    }

    private RealtimeEventEnvelope toEnvelope(ChatRealtimeEvent row) {
        return RealtimeEventEnvelope.builder()
                .eventId(row.getEventId())
                .eventType(row.getEventType())
                .bizType(row.getBizType())
                .targetUid(row.getTargetUid())
                .roomId(row.getRoomId())
                .msgId(row.getMsgId())
                .occurredAt(toDate(row.getOccurredAt()))
                .payload(readPayload(row.getPayloadJson()))
                .build();
    }

    private String writePayload(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    private Object readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, Object.class);
        } catch (Exception e) {
            log.warn("chat_realtime_event_parse_failed payloadJson={}", payloadJson, e);
            return null;
        }
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private long normalizeWatermark(Long lastEventId) {
        return lastEventId == null || lastEventId <= 0 ? 0L : lastEventId;
    }

    private Long normalizeLatestEventId(Long latestEventId) {
        return latestEventId == null || latestEventId <= 0 ? 0L : latestEventId;
    }

    private LocalDateTime toLocalDateTime(Date occurredAt) {
        Date safeOccurredAt = occurredAt == null ? new Date() : occurredAt;
        return LocalDateTime.ofInstant(safeOccurredAt.toInstant(), ZoneId.systemDefault());
    }

    private Date toDate(LocalDateTime occurredAt) {
        if (occurredAt == null) {
            return null;
        }
        return Date.from(occurredAt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
