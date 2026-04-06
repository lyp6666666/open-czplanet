package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RefundRequestMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRefundServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Mock
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Mock
    private BrokerageOrderMapper brokerageOrderMapper;
    @Mock
    private CollaborationProposalMapper collaborationProposalMapper;
    @Mock
    private RefundRequestMapper refundRequestMapper;
    @Mock
    private ChatService chatService;
    @Mock
    private CourseEnrollmentMapper courseEnrollmentMapper;

    @InjectMocks
    private ChatRefundService chatRefundService;

    @Test
    void applyChatRefundShouldSendSystemMsgAndCloseRoom() {
        Room room = Room.builder().id(10L).teacherProfileId(100L).studentProfileId(200L).status(1).build();
        when(roomMapper.selectById(10L)).thenReturn(room);
        when(teacherProfileLiteMapper.selectUserIdById(100L)).thenReturn(1L);
        when(studentProfileLiteMapper.selectUserIdById(200L)).thenReturn(2L);

        BrokerageOrder order = BrokerageOrder.builder().id(9L).roomId(10L).payerUid(1L).amountFen(19900L).status("PAID").build();
        when(brokerageOrderMapper.selectPaidByRoomId(10L)).thenReturn(order);
        when(refundRequestMapper.selectPendingByBrokerageOrderId(9L)).thenReturn(null);
        when(brokerageOrderMapper.lockForRefund(9L, "REFUND_REVIEW")).thenReturn(1);
        doAnswer(inv -> {
            com.ai.tutor.videocallimservice.chat.domain.entity.RefundRequest rr = inv.getArgument(0);
            rr.setId(1000L);
            return 1;
        }).when(refundRequestMapper).insert(any());

        ArgumentCaptor<ChatMessageReq> msgCaptor = ArgumentCaptor.forClass(ChatMessageReq.class);
        when(chatService.sendMsg(msgCaptor.capture(), eq(1L))).thenReturn(1L);
        ChatMessageResp.Message msg = new ChatMessageResp.Message();
        msg.setId(1L);
        msg.setRoomId(10L);
        when(chatService.getMsgResp(eq(1L), eq(1L))).thenReturn(ChatMessageResp.builder().message(msg).build());

        ChatMessageResp resp = chatRefundService.applyChatRefund(10L, "不合适", 1L);

        assertThat(resp.getMessage().getRoomId()).isEqualTo(10L);
        ChatMessageReq sent = msgCaptor.getValue();
        assertThat(sent.getRoomId()).isEqualTo(10L);
        assertThat(sent.getMsgType()).isEqualTo(8);
        assertThat(sent.getBody()).isInstanceOf(SystemMsgReq.class);
    }
}
