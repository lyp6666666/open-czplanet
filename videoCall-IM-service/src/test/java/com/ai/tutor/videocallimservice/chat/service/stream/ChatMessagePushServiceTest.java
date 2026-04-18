package com.ai.tutor.videocallimservice.chat.service.stream;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;
import com.ai.tutor.videocallimservice.chat.service.strategy.MsgHandlerFactory;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessagePushServiceTest {

    @Test
    void pushMessageByIdShouldSendToBothParticipants() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);
        @SuppressWarnings("rawtypes")
        AbstractMsgHandler handler = mock(AbstractMsgHandler.class);
        when(handler.showMsg(any(Message.class))).thenReturn(Map.of("content", "你好"));
        MsgHandlerFactory.register(1, handler);

        Message msg = Message.builder()
                .id(9001L)
                .roomId(7001L)
                .fromUid(1001L)
                .toUid(2001L)
                .status(0)
                .type(1)
                .content("你好")
                .createTime(LocalDateTime.of(2026, 4, 18, 20, 0, 0))
                .build();
        when(messageMapper.getById(9001L)).thenReturn(msg);

        ChatMessagePushService service = new ChatMessagePushService();
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);

        assertTrue(service.pushMessageById(9001L));
        verify(sseSessionManager).sendToUid(eq(2001L), eq("message"), any());
        verify(sseSessionManager).sendToUid(eq(1001L), eq("message"), any());
    }

    @Test
    void pushMessageByIdShouldIgnoreInvalidMessage() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        SseSessionManager sseSessionManager = mock(SseSessionManager.class);
        when(messageMapper.getById(9002L)).thenReturn(Message.builder().id(9002L).status(1).build());

        ChatMessagePushService service = new ChatMessagePushService();
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "sseSessionManager", sseSessionManager);

        assertFalse(service.pushMessageById(9002L));
        verify(sseSessionManager, never()).sendToUid(eq(2001L), eq("message"), any());
        verify(sseSessionManager, never()).sendToUid(eq(1001L), eq("message"), any());
    }
}
