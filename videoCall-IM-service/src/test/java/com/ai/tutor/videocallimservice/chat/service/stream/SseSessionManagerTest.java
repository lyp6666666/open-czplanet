package com.ai.tutor.videocallimservice.chat.service.stream;

import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamMessageEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamDeliveryEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamReadEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamTypingEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventEnvelope;
import com.ai.tutor.videocallimservice.chat.service.realtime.RealtimeEventStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SseSessionManagerTest {

    @Test
    void shouldBuildReplayEventForMessagePush() {
        SseSessionManager manager = new SseSessionManager();
        ChatStreamMessageEvent event = new ChatStreamMessageEvent();
        event.setMsgId(9001L);
        event.setRoomId(7001L);
        event.setFromUid(1001L);
        event.setToUid(2001L);
        event.setBody(Map.of("content", "你好"));

        manager.sendToUid(2001L, "message", event);

        List<RealtimeEventEnvelope> replayEvents = manager.listReplayEventsAfter(2001L, 0L);
        assertThat(replayEvents).hasSize(1);
        RealtimeEventEnvelope replayEvent = replayEvents.get(0);
        assertThat(replayEvent.getEventType()).isEqualTo("chat.message.created");
        assertThat(replayEvent.getBizType()).isEqualTo("chat");
        assertThat(replayEvent.getRoomId()).isEqualTo(7001L);
        assertThat(replayEvent.getMsgId()).isEqualTo(9001L);
    }

    @Test
    void shouldMapApplicationCreatedToUnifiedEventType() {
        SseSessionManager manager = new SseSessionManager();

        manager.sendToUid(3001L, "application", Map.of(
                "type", "CREATED",
                "applicationId", 8101L,
                "status", "PENDING"
        ));

        List<RealtimeEventEnvelope> replayEvents = manager.listReplayEventsAfter(3001L, 0L);
        assertThat(replayEvents).hasSize(1);
        assertThat(replayEvents.get(0).getEventType()).isEqualTo("application.created");
        assertThat(replayEvents.get(0).getBizType()).isEqualTo("application");
    }

    @Test
    void shouldTrimReplayBufferToConfiguredWindow() {
        SseSessionManager manager = new SseSessionManager();

        for (int i = 1; i <= 220; i++) {
            ChatStreamMessageEvent event = new ChatStreamMessageEvent();
            event.setMsgId((long) i);
            event.setRoomId(8001L);
            event.setFromUid(1001L);
            event.setToUid(2001L);
            manager.sendToUid(2001L, "message", event);
        }

        List<RealtimeEventEnvelope> replayEvents = manager.listReplayEventsAfter(2001L, 0L);
        assertThat(replayEvents).hasSize(200);
        assertThat(replayEvents.get(0).getMsgId()).isEqualTo(21L);
        assertThat(replayEvents.get(replayEvents.size() - 1).getMsgId()).isEqualTo(220L);
    }

    @Test
    void shouldBuildReplayEventForReadReceiptPush() {
        SseSessionManager manager = new SseSessionManager();
        ChatStreamReadEvent event = new ChatStreamReadEvent();
        event.setRoomId(7001L);
        event.setReaderUid(2001L);
        event.setLastReadMsgId(9001L);

        manager.sendToUid(1001L, "read", event);

        List<RealtimeEventEnvelope> replayEvents = manager.listReplayEventsAfter(1001L, 0L);
        assertThat(replayEvents).hasSize(1);
        RealtimeEventEnvelope replayEvent = replayEvents.get(0);
        assertThat(replayEvent.getEventType()).isEqualTo("chat.read.updated");
        assertThat(replayEvent.getBizType()).isEqualTo("chat");
        assertThat(replayEvent.getRoomId()).isEqualTo(7001L);
        assertThat(replayEvent.getMsgId()).isEqualTo(9001L);
    }

    @Test
    void shouldBuildReplayEventForDeliveryReceiptPush() {
        SseSessionManager manager = new SseSessionManager();
        ChatStreamDeliveryEvent event = new ChatStreamDeliveryEvent();
        event.setRoomId(7001L);
        event.setDeliverUid(2001L);
        event.setLastDeliveredMsgId(9001L);

        manager.sendToUid(1001L, "delivery", event);

        List<RealtimeEventEnvelope> replayEvents = manager.listReplayEventsAfter(1001L, 0L);
        assertThat(replayEvents).hasSize(1);
        RealtimeEventEnvelope replayEvent = replayEvents.get(0);
        assertThat(replayEvent.getEventType()).isEqualTo("chat.delivery.updated");
        assertThat(replayEvent.getBizType()).isEqualTo("chat");
        assertThat(replayEvent.getRoomId()).isEqualTo(7001L);
        assertThat(replayEvent.getMsgId()).isEqualTo(9001L);
    }

    @Test
    void shouldNotStoreEphemeralTypingEventInReplayBuffer() {
        SseSessionManager manager = new SseSessionManager();
        ChatStreamTypingEvent event = new ChatStreamTypingEvent();
        event.setRoomId(7001L);
        event.setTypingUid(2001L);
        event.setTyping(true);

        manager.sendEphemeralToUid(1001L, "typing", event);

        List<RealtimeEventEnvelope> replayEvents = manager.listReplayEventsAfter(1001L, 0L);
        assertThat(replayEvents).isEmpty();
    }

    @Test
    void shouldExposePersistedLatestEventIdWhenMemoryReplayIsEmpty() {
        SseSessionManager manager = new SseSessionManager();
        RealtimeEventStoreService realtimeEventStoreService = mock(RealtimeEventStoreService.class);
        when(realtimeEventStoreService.getLatestEventId(2001L)).thenReturn(9527L);
        ReflectionTestUtils.setField(manager, "realtimeEventStoreService", realtimeEventStoreService);

        assertThat(manager.getLatestEventId(2001L)).isEqualTo(9527L);
    }

    @Test
    void shouldReportUserOnlineWhenSseSessionExists() {
        SseSessionManager manager = new SseSessionManager();

        manager.connectV2(2001L, "web-2001", null);

        assertThat(manager.listPresence(List.of(2001L)))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getUid()).isEqualTo(2001L);
                    assertThat(item.getOnline()).isTrue();
                    assertThat(item.getLastOnlineAt()).isNull();
                });
    }

    @Test
    void shouldRecordLastOnlineTimeWhenLastSessionDisconnects() {
        SseSessionManager manager = new SseSessionManager();
        SseEmitter emitter = manager.connect(3001L);
        @SuppressWarnings("unchecked")
        Map<Long, List<Object>> emittersByUid = (Map<Long, List<Object>>) ReflectionTestUtils.getField(manager, "emittersByUid");
        assertThat(emittersByUid).isNotNull();
        Object holder = emittersByUid.get(3001L).getFirst();

        ReflectionTestUtils.invokeMethod(manager, "remove", 3001L, holder);

        assertThat(emitter).isNotNull();
        assertThat(manager.listPresence(List.of(3001L)))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getOnline()).isFalse();
                    assertThat(item.getLastOnlineAt()).isNotNull().isBeforeOrEqualTo(new Date());
                });
    }

    @Test
    void shouldKeepLastOnlineTimeWhenThereAreRepeatedBatchUids() {
        SseSessionManager manager = new SseSessionManager();
        @SuppressWarnings("unchecked")
        Map<Long, Date> lastOfflineAtByUid = (Map<Long, Date>) ReflectionTestUtils.getField(manager, "lastOfflineAtByUid");
        assertThat(lastOfflineAtByUid).isNotNull();
        Date lastOnlineAt = new Date();
        lastOfflineAtByUid.put(4001L, lastOnlineAt);

        List<?> presenceList = manager.listPresence(List.of(4001L, 4001L));

        assertThat(presenceList).hasSize(1);
        assertThat(manager.listPresence(List.of(4001L)).getFirst().getLastOnlineAt()).isEqualTo(lastOnlineAt);
    }
}
