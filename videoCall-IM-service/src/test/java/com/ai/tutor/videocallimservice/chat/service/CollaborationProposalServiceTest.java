package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.RespondCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.StudentJobPostingLiteMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    void createShouldRejectWhenInfoFeeNotPaid() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);
        TutorApplicationService tutorApplicationService = mock(TutorApplicationService.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(svc, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "tutorApplicationService", tutorApplicationService);
        ReflectionTestUtils.setField(svc, "chatService", mock(ChatService.class));
        ReflectionTestUtils.setField(svc, "brokerageOrderService", mock(BrokerageOrderService.class));
        ReflectionTestUtils.setField(svc, "courseEnrollmentService", mock(CourseEnrollmentService.class));
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(svc, "studentJobPostingLiteMapper", mock(StudentJobPostingLiteMapper.class));

        when(roomMapper.selectById(100L)).thenReturn(Room.builder()
                .id(100L)
                .status(1)
                .teacherProfileId(11L)
                .studentProfileId(22L)
                .build());
        when(teacherProfileLiteMapper.selectUserIdById(11L)).thenReturn(1001L);
        when(studentProfileLiteMapper.selectUserIdById(22L)).thenReturn(2001L);
        ImUser teacher = new ImUser();
        teacher.setId(1001L);
        teacher.setUserType(1);
        teacher.setStatus(0);
        ImUser student = new ImUser();
        student.setId(2001L);
        student.setUserType(2);
        student.setStatus(0);
        when(imUserMapper.selectById(1001L)).thenReturn(teacher);
        when(imUserMapper.selectById(2001L)).thenReturn(student);
        org.mockito.Mockito.doThrow(new BusinessException(40000, "教师支付信息费后，双方才能继续沟通并发起合作或授课申请"))
                .when(tutorApplicationService).assertRoomReadyForScheduling(100L, 1001L);

        CreateCollaborationProposalReq req = new CreateCollaborationProposalReq();
        req.setRoomId(100L);
        req.setPricePerHour("200 元/小时");
        req.setClassTime("周三 19:00-21:00");
        req.setFrequencyPerWeek(2);

        assertThatThrownBy(() -> svc.createAndSend(req, 1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("教师支付信息费后");
        verify(collaborationProposalMapper, never()).insert(any());
    }

    @Test
    void createShouldPersistTrialFieldsAndClientRequestId() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);
        TutorApplicationService tutorApplicationService = mock(TutorApplicationService.class);
        ChatService chatService = mock(ChatService.class);
        BizKpiMetrics bizKpiMetrics = mock(BizKpiMetrics.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(svc, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "tutorApplicationService", tutorApplicationService);
        ReflectionTestUtils.setField(svc, "chatService", chatService);
        ReflectionTestUtils.setField(svc, "brokerageOrderService", mock(BrokerageOrderService.class));
        ReflectionTestUtils.setField(svc, "courseEnrollmentService", mock(CourseEnrollmentService.class));
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(svc, "studentJobPostingLiteMapper", mock(StudentJobPostingLiteMapper.class));
        ReflectionTestUtils.setField(svc, "bizKpiMetrics", bizKpiMetrics);

        when(roomMapper.selectById(100L)).thenReturn(Room.builder().id(100L).status(1).teacherProfileId(11L).studentProfileId(22L).build());
        when(teacherProfileLiteMapper.selectUserIdById(11L)).thenReturn(1001L);
        when(studentProfileLiteMapper.selectUserIdById(22L)).thenReturn(2001L);
        ImUser teacher = new ImUser();
        teacher.setId(1001L);
        teacher.setStatus(0);
        ImUser student = new ImUser();
        student.setId(2001L);
        student.setStatus(0);
        when(imUserMapper.selectById(1001L)).thenReturn(teacher);
        when(imUserMapper.selectById(2001L)).thenReturn(student);
        org.mockito.Mockito.doNothing().when(tutorApplicationService).assertRoomReadyForScheduling(100L, 1001L);
        when(collaborationProposalMapper.selectLatestByRoomId(100L)).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> {
            CollaborationProposal proposal = inv.getArgument(0);
            proposal.setId(77L);
            return null;
        }).when(collaborationProposalMapper).insert(any());
        when(chatService.sendMsg(any(ChatMessageReq.class), eq(1001L))).thenReturn(9001L);

        CreateCollaborationProposalReq req = new CreateCollaborationProposalReq();
        req.setRoomId(100L);
        req.setPricePerHour("200 元/小时");
        req.setTrialStartAt(1771412400000L);
        req.setTrialEndAt(1771419600000L);
        req.setRemark("试课备注");
        req.setClientRequestId("client-1");

        Long msgId = svc.createAndSend(req, 1001L);

        assertThat(msgId).isEqualTo(9001L);
        org.mockito.ArgumentCaptor<CollaborationProposal> captor = org.mockito.ArgumentCaptor.forClass(CollaborationProposal.class);
        verify(collaborationProposalMapper).insert(captor.capture());
        assertThat(captor.getValue().getTrialStartAt()).isNotNull();
        assertThat(captor.getValue().getTrialEndAt()).isNotNull();
        assertThat(captor.getValue().getRemark()).isEqualTo("试课备注");
        assertThat(captor.getValue().getExpireAt()).isNotNull();
        assertThat(captor.getValue().getClientRequestId()).isEqualTo("client-1");
        verify(bizKpiMetrics).incTrialProposalCreated("teacher");
    }

    @Test
    void createShouldCheckTrialScheduleConflictBeforeInsert() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        ImUserMapper imUserMapper = mock(ImUserMapper.class);
        TeacherProfileLiteMapper teacherProfileLiteMapper = mock(TeacherProfileLiteMapper.class);
        StudentProfileLiteMapper studentProfileLiteMapper = mock(StudentProfileLiteMapper.class);
        TutorApplicationService tutorApplicationService = mock(TutorApplicationService.class);
        AppointmentInternalClient appointmentInternalClient = mock(AppointmentInternalClient.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(svc, "imUserMapper", imUserMapper);
        ReflectionTestUtils.setField(svc, "teacherProfileLiteMapper", teacherProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "studentProfileLiteMapper", studentProfileLiteMapper);
        ReflectionTestUtils.setField(svc, "tutorApplicationService", tutorApplicationService);
        ReflectionTestUtils.setField(svc, "appointmentInternalClient", appointmentInternalClient);
        ReflectionTestUtils.setField(svc, "chatService", mock(ChatService.class));
        ReflectionTestUtils.setField(svc, "brokerageOrderService", mock(BrokerageOrderService.class));
        ReflectionTestUtils.setField(svc, "courseEnrollmentService", mock(CourseEnrollmentService.class));
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(svc, "studentJobPostingLiteMapper", mock(StudentJobPostingLiteMapper.class));

        when(roomMapper.selectById(100L)).thenReturn(Room.builder().id(100L).status(1).teacherProfileId(11L).studentProfileId(22L).build());
        when(teacherProfileLiteMapper.selectUserIdById(11L)).thenReturn(1001L);
        when(studentProfileLiteMapper.selectUserIdById(22L)).thenReturn(2001L);
        ImUser teacher = new ImUser();
        teacher.setId(1001L);
        teacher.setStatus(0);
        ImUser student = new ImUser();
        student.setId(2001L);
        student.setStatus(0);
        when(imUserMapper.selectById(1001L)).thenReturn(teacher);
        when(imUserMapper.selectById(2001L)).thenReturn(student);
        org.mockito.Mockito.doNothing().when(tutorApplicationService).assertRoomReadyForScheduling(100L, 1001L);
        when(collaborationProposalMapper.selectLatestByRoomId(100L)).thenReturn(null);
        org.mockito.Mockito.doThrow(new BusinessException(40000, "与对方日程冲突"))
                .when(appointmentInternalClient)
                .assertNoScheduleConflict(1001L, 2001L, 1771412400000L, 1771419600000L);

        CreateCollaborationProposalReq req = new CreateCollaborationProposalReq();
        req.setRoomId(100L);
        req.setPricePerHour("200 元/小时");
        req.setTrialStartAt(1771412400000L);
        req.setTrialEndAt(1771419600000L);

        assertThatThrownBy(() -> svc.createAndSend(req, 1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("与对方日程冲突");
        verify(collaborationProposalMapper, never()).insert(any());
        verify(appointmentInternalClient).assertNoScheduleConflict(1001L, 2001L, 1771412400000L, 1771419600000L);
    }

    @Test
    void respondShouldRejectExpiredPendingProposal() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        ChatService chatService = mock(ChatService.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "chatService", chatService);
        ReflectionTestUtils.setField(svc, "brokerageOrderService", mock(BrokerageOrderService.class));
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(svc, "studentJobPostingLiteMapper", mock(StudentJobPostingLiteMapper.class));
        ReflectionTestUtils.setField(svc, "courseEnrollmentService", mock(CourseEnrollmentService.class));

        CollaborationProposal proposal = CollaborationProposal.builder()
                .id(10L)
                .roomId(100L)
                .fromUid(1L)
                .toUid(2L)
                .status("PENDING")
                .createTime(LocalDateTime.now().minusHours(13))
                .build();
        when(collaborationProposalMapper.selectById(10L)).thenReturn(proposal);

        RespondCollaborationProposalReq req = new RespondCollaborationProposalReq();
        req.setAction("ACCEPT");

        assertThatThrownBy(() -> svc.respondAndSend(10L, req, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已超过12小时");
        verify(collaborationProposalMapper, never()).updateStatus(any(), any(), any(), any());
        verify(chatService, never()).sendMsg(any(), any());
    }

    @Test
    void respondShouldBeIdempotentWhenAlreadyAcceptedByReceiver() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        ChatService chatService = mock(ChatService.class);
        BrokerageOrderService brokerageOrderService = mock(BrokerageOrderService.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "chatService", chatService);
        ReflectionTestUtils.setField(svc, "brokerageOrderService", brokerageOrderService);
        ReflectionTestUtils.setField(svc, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(svc, "studentJobPostingLiteMapper", mock(StudentJobPostingLiteMapper.class));
        ReflectionTestUtils.setField(svc, "courseEnrollmentService", mock(CourseEnrollmentService.class));

        CollaborationProposal accepted = CollaborationProposal.builder()
                .id(10L)
                .roomId(100L)
                .fromUid(1L)
                .toUid(2L)
                .status("ACCEPTED")
                .build();
        when(collaborationProposalMapper.selectById(10L)).thenReturn(accepted);
        when(collaborationProposalMapper.updateStatus(eq(10L), eq("ACCEPTED"), eq(2L), any())).thenReturn(0);
        when(chatService.sendMsg(any(ChatMessageReq.class), eq(2L))).thenReturn(9002L);
        when(brokerageOrderService.hasPaidOrderInRoom(100L)).thenReturn(true);

        RespondCollaborationProposalReq req = new RespondCollaborationProposalReq();
        req.setAction("ACCEPT");

        Long msgId = svc.respondAndSend(10L, req, 2L);

        assertThat(msgId).isEqualTo(9002L);
        verify(chatService).sendMsg(any(ChatMessageReq.class), eq(2L));
    }

    @Test
    void processExpiredProposalsShouldCountOnlySuccessfulTransitions() {
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        BizKpiMetrics bizKpiMetrics = mock(BizKpiMetrics.class);

        CollaborationProposalService svc = new CollaborationProposalService();
        ReflectionTestUtils.setField(svc, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(svc, "bizKpiMetrics", bizKpiMetrics);

        when(collaborationProposalMapper.selectExpiredPendingIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(java.util.List.of(11L, 12L));
        when(collaborationProposalMapper.updateStatus(eq(11L), eq("EXPIRED"), eq(0L), any(LocalDateTime.class)))
                .thenReturn(1);
        when(collaborationProposalMapper.updateStatus(eq(12L), eq("EXPIRED"), eq(0L), any(LocalDateTime.class)))
                .thenReturn(0);

        svc.processExpiredProposals();

        verify(bizKpiMetrics, times(1)).incTrialProposalExpired();
    }
}
