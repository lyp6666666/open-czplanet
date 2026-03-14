package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.RespondCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.StudentJobPostingLiteMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollaborationProposalServiceTest {

    @Test
    void acceptShouldNotSendBrokerageRequiredWhenRoomAlreadyPaid() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        ChatService chatService = mock(ChatService.class);
        BrokerageOrderService brokerageOrderService = mock(BrokerageOrderService.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        StudentJobPostingLiteMapper studentJobPostingLiteMapper = mock(StudentJobPostingLiteMapper.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "chatService", chatService);
        ReflectionTestUtils.setField(svc, "brokerageOrderService", brokerageOrderService);
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(svc, "studentJobPostingLiteMapper", studentJobPostingLiteMapper);

        CollaborationProposal proposal = CollaborationProposal.builder()
                .id(10L)
                .roomId(100L)
                .fromUid(1L)
                .toUid(2L)
                .status("PENDING")
                .build();

        when(collaborationProposalMapper.selectById(10L)).thenReturn(proposal);
        when(collaborationProposalMapper.updateStatus(eq(10L), eq("ACCEPTED"), eq(2L), any())).thenReturn(1);
        when(chatService.sendMsg(any(ChatMessageReq.class), eq(2L))).thenReturn(9001L);
        when(brokerageOrderService.hasPaidOrderInRoom(100L)).thenReturn(true);
        when(tutorApplicationMapper.selectLatestByRoomId(100L)).thenReturn(null);

        RespondCollaborationProposalReq req = new RespondCollaborationProposalReq();
        req.setAction("ACCEPT");
        Long msgId = svc.respondAndSend(10L, req, 2L);

        assertThat(msgId).isEqualTo(9001L);
        verify(brokerageOrderService, never()).getOrCreateByProposal(any(), any());
        verify(brokerageOrderService, never()).sendBrokerageRequired(any(), any(), any(), any());
    }
}
