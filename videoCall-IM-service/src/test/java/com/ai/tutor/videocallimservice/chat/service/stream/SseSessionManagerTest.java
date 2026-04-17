package com.ai.tutor.videocallimservice.chat.service.stream;

import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatStreamMessageEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
