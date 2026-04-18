package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageSearchReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.service.impl.ChatServiceImpl;
import com.ai.tutor.videocallimservice.chat.service.strategy.msg.TextMsgHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceImplSearchTest {

    @Test
    void searchMsgPageShouldReturnNewestMatchesFirst() {
        TextMsgHandler textMsgHandler = new TextMsgHandler();
        ReflectionTestUtils.invokeMethod(textMsgHandler, "init");

        MessageMapper messageMapper = mock(MessageMapper.class);
        ChatServiceImpl service = new ChatServiceImpl();
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);

        Message older = Message.builder()
                .id(101L)
                .roomId(10L)
                .fromUid(2001L)
                .toUid(3001L)
                .type(1)
                .status(0)
                .content("数学提分方案")
                .createTime(LocalDateTime.of(2026, 4, 18, 10, 0, 0))
                .build();
        Message newer = Message.builder()
                .id(105L)
                .roomId(10L)
                .fromUid(3001L)
                .toUid(2001L)
                .type(1)
                .status(0)
                .content("数学作业我看过了")
                .createTime(LocalDateTime.of(2026, 4, 18, 10, 1, 0))
                .build();

        when(messageMapper.searchCursorPage(eq(10L), any(ChatMessageSearchReq.class)))
                .thenReturn(new CursorPageBaseResp<>("101", true, List.of(older, newer)));

        ChatMessageSearchReq req = ChatMessageSearchReq.builder()
                .roomId(10L)
                .keyword(" 数学 ")
                .build();
        req.setPageSize(20);

        CursorPageBaseResp<ChatMessageResp> resp = service.searchMsgPage(req, 2001L);

        assertThat(req.getKeyword()).isEqualTo("数学");
        assertThat(resp.getList()).hasSize(2);
        assertThat(resp.getList().get(0).getMessage().getId()).isEqualTo(105L);
        assertThat(resp.getList().get(1).getMessage().getId()).isEqualTo(101L);
        assertThat(String.valueOf(resp.getList().get(0).getMessage().getBody())).contains("数学");
    }
}
