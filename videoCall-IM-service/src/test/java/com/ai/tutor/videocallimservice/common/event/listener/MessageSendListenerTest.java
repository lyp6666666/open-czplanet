package com.ai.tutor.videocallimservice.common.event.listener;

import com.ai.tutor.videocallimservice.chat.service.mq.MQProducer;
import com.ai.tutor.videocallimservice.chat.service.stream.ChatMessagePushService;
import com.ai.tutor.videocallimservice.common.event.MessageSendEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageSendListenerTest {

    @Test
    void shouldFallbackToDirectPushWhenMqSendFails() {
        MQProducer mqProducer = mock(MQProducer.class);
        ChatMessagePushService chatMessagePushService = mock(ChatMessagePushService.class);
        doThrow(new RuntimeException("mq down")).when(mqProducer).sendSecureMsg(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());

        MessageSendListener listener = new MessageSendListener();
        ReflectionTestUtils.setField(listener, "mqProducer", mqProducer);
        ReflectionTestUtils.setField(listener, "chatMessagePushService", chatMessagePushService);

        listener.messageRoute(new MessageSendEvent(this, 9527L));

        verify(chatMessagePushService).pushMessageById(9527L);
    }
}
