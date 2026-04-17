package com.ai.tutor.videocallimservice.chat.service.realtime;

import com.ai.tutor.videocallimservice.chat.domain.entity.ChatRealtimeEvent;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventEnvelope;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventSyncResp;
import com.ai.tutor.videocallimservice.chat.mapper.ChatRealtimeEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeEventStoreServiceTest {

    @Mock
    private ChatRealtimeEventMapper chatRealtimeEventMapper;

    @InjectMocks
    private RealtimeEventStoreService realtimeEventStoreService;

    @Test
    void saveShouldPersistPayloadAsJson() {
        ReflectionTestUtils.setField(realtimeEventStoreService, "objectMapper", new ObjectMapper());
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .eventId(9001L)
                .targetUid(2001L)
                .eventType("application.created")
                .bizType("application")
                .occurredAt(new Date())
                .payload(Map.of("applicationId", 9527L, "status", "PENDING"))
                .build();

        realtimeEventStoreService.save(envelope);

        ArgumentCaptor<ChatRealtimeEvent> captor = ArgumentCaptor.forClass(ChatRealtimeEvent.class);
        verify(chatRealtimeEventMapper).insertEvent(captor.capture());
        ChatRealtimeEvent saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(9001L);
        assertThat(saved.getTargetUid()).isEqualTo(2001L);
        assertThat(saved.getPayloadJson()).contains("\"applicationId\":9527");
        assertThat(saved.getPayloadJson()).contains("\"status\":\"PENDING\"");
    }

    @Test
    void syncShouldReturnAscendingEventsAndCursor() {
        ReflectionTestUtils.setField(realtimeEventStoreService, "objectMapper", new ObjectMapper());
        when(chatRealtimeEventMapper.listAfter(eq(2001L), eq(800L), eq(3))).thenReturn(List.of(
                ChatRealtimeEvent.builder()
                        .eventId(801L)
                        .targetUid(2001L)
                        .eventType("application.created")
                        .bizType("application")
                        .occurredAt(LocalDateTime.of(2026, 4, 17, 10, 0))
                        .payloadJson("{\"applicationId\":1,\"status\":\"PENDING\"}")
                        .build(),
                ChatRealtimeEvent.builder()
                        .eventId(802L)
                        .targetUid(2001L)
                        .eventType("application.decided")
                        .bizType("application")
                        .occurredAt(LocalDateTime.of(2026, 4, 17, 10, 1))
                        .payloadJson("{\"applicationId\":1,\"status\":\"ACCEPTED\"}")
                        .build(),
                ChatRealtimeEvent.builder()
                        .eventId(803L)
                        .targetUid(2001L)
                        .eventType("chat.message.created")
                        .bizType("chat")
                        .occurredAt(LocalDateTime.of(2026, 4, 17, 10, 2))
                        .payloadJson("{\"msgId\":3001}")
                        .build()
        ));
        when(chatRealtimeEventMapper.selectLatestEventId(2001L)).thenReturn(803L);

        RealtimeEventSyncResp resp = realtimeEventStoreService.sync(2001L, 800L, 2);

        assertThat(resp.getIsLast()).isFalse();
        assertThat(resp.getLatestEventId()).isEqualTo(803L);
        assertThat(resp.getCursor()).isEqualTo(802L);
        assertThat(resp.getList()).hasSize(2);
        assertThat(resp.getList().get(0).getEventId()).isEqualTo(801L);
        assertThat(resp.getList().get(1).getEventType()).isEqualTo("application.decided");
        assertThat(resp.getList().get(1).getPayload()).isEqualTo(Map.of("applicationId", 1, "status", "ACCEPTED"));
    }

    @Test
    void getLatestEventIdShouldReturnZeroWhenMapperHasNoData() {
        when(chatRealtimeEventMapper.selectLatestEventId(any())).thenReturn(null);

        assertThat(realtimeEventStoreService.getLatestEventId(2001L)).isEqualTo(0L);
    }
}
