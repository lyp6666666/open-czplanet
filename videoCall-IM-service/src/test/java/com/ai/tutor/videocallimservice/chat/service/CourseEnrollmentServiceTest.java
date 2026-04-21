package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import com.ai.tutor.videocallimservice.chat.domain.entity.RefundRequest;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ApplyTrialRefundReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SubmitTrialResultReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseDetailVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RefundRequestMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import com.ai.tutor.videocallimservice.integration.feign.AppointmentInternalFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseEnrollmentServiceTest {

    @Test
    void onCollaborationAcceptedShouldPersistOnlineCourseSnapshot() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        AppointmentInternalClient appointmentInternalClient = mock(AppointmentInternalClient.class);

        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", refundRequestMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "appointmentInternalClient", appointmentInternalClient);

        when(courseEnrollmentMapper.selectLatestByRoomId(88L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .status("COMMUNICATING")
                .build());
        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .courseName("线上一对一｜200 元/小时｜每周三 19:00-21:00")
                .lessonPrice("200 元/小时")
                .trialStartAt(java.time.LocalDateTime.of(2026, 4, 23, 19, 0))
                .trialEndAt(java.time.LocalDateTime.of(2026, 4, 23, 21, 0))
                .status("TRIALING")
                .build());
        when(tutorApplicationMapper.selectLatestByRoomId(88L)).thenReturn(TutorApplication.builder()
                .id(501L)
                .roomId(88L)
                .teachingMode("ONLINE")
                .build());
        when(collaborationProposalMapper.selectById(9001L)).thenReturn(CollaborationProposal.builder()
                .id(9001L)
                .fromUid(1001L)
                .pricePerHour("200 元/小时")
                .classTime("每周三 19:00-21:00")
                .frequencyPerWeek(2)
                .trialStartAt(java.time.LocalDateTime.of(2026, 4, 23, 19, 0))
                .trialEndAt(java.time.LocalDateTime.of(2026, 4, 23, 21, 0))
                .build());
        when(courseEnrollmentMapper.startOnlineCourse(eq(66L), eq("COMMUNICATING"), eq(9001L), eq("ONLINE"), eq("线上一对一｜200 元/小时｜每周三 19:00-21:00"),
                eq("每周三 19:00-21:00"), eq(2), eq("200 元/小时"), any(), any())).thenReturn(1);

        service.onCollaborationAccepted(88L, 9001L);

        verify(courseEnrollmentMapper).startOnlineCourse(
                eq(66L),
                eq("COMMUNICATING"),
                eq(9001L),
                eq("ONLINE"),
                eq("线上一对一｜200 元/小时｜每周三 19:00-21:00"),
                eq("每周三 19:00-21:00"),
                eq(2),
                eq("200 元/小时"),
                any(),
                any()
        );
        verify(appointmentInternalClient).createAcceptedTrialEvent(any(AppointmentInternalFeignClient.InternalTrialEventRequest.class));
    }

    @Test
    void getCourseByRoomShouldRequireParticipant() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);

        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", refundRequestMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);

        when(courseEnrollmentMapper.selectLatestByRoomId(88L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .teachingMode("ONLINE")
                .courseName("线上长期课程")
                .status("TRIALING")
                .build());

        CourseDetailVO detail = service.getCourseByRoom(88L, 1001L);
        assertThat(detail.getCourseId()).isEqualTo(66L);
        assertThat(detail.getCourseName()).isEqualTo("线上长期课程");

        assertThatThrownBy(() -> service.getCourseByRoom(88L, 3001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void applyTrialRefundShouldUseOfflineVideoAndRefund80Percent() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        RefundRequestMapper refundRequestMapper = mock(RefundRequestMapper.class);
        BrokerageOrderMapper brokerageOrderMapper = mock(BrokerageOrderMapper.class);
        CollaborationProposalMapper collaborationProposalMapper = mock(CollaborationProposalMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);

        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", refundRequestMapper);
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", brokerageOrderMapper);
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", collaborationProposalMapper);
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);

        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .teachingMode("OFFLINE")
                .status("TRIALING")
                .trialEndAt(java.time.LocalDateTime.now().plusDays(1))
                .build());
        BrokerageOrder order = BrokerageOrder.builder()
                .id(99L)
                .amountFen(10000L)
                .status("PAID")
                .build();
        when(brokerageOrderMapper.selectByApplicationId(501L)).thenReturn(order);
        when(brokerageOrderMapper.lockForRefund(99L, "TRIAL_REFUND_REVIEW")).thenReturn(1);
        org.mockito.ArgumentCaptor<RefundRequest> captor = org.mockito.ArgumentCaptor.forClass(RefundRequest.class);
        when(refundRequestMapper.insert(captor.capture())).thenAnswer(inv -> {
            RefundRequest request = inv.getArgument(0);
            request.setId(10L);
            return 1;
        });

        ApplyTrialRefundReq req = new ApplyTrialRefundReq();
        req.setReason("试课不合适");
        req.setEvidenceImageUrls(java.util.List.of("https://img.example.com/a.png"));
        req.setEvidenceVideoUrl("https://video.example.com/wechat.mp4");
        req.setEvidenceVideoDurationSeconds(45);

        Long requestId = service.applyTrialRefund(66L, req, 1001L);

        assertThat(requestId).isEqualTo(10L);
        RefundRequest saved = captor.getValue();
        assertThat(saved.getRefundPercent()).isEqualTo(80);
        assertThat(saved.getRefundAmountFen()).isEqualTo(8000L);
        assertThat(saved.getEvidenceVideoUrl()).isEqualTo("https://video.example.com/wechat.mp4");
        assertThat(saved.getEvidenceVideoDeleteStatus()).isEqualTo("PENDING_DELETE");
        verify(tutorApplicationMapper).updateChatAccessStatus(501L, "NONE");
        verify(roomMapper).closeRoom(88L);
    }

    @Test
    void applyTrialRefundShouldRejectOnlineInfoFeeRefund() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", mock(RefundRequestMapper.class));
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mock(BrokerageOrderMapper.class));
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", mock(CollaborationProposalMapper.class));
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(service, "roomMapper", mock(RoomMapper.class));
        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .teacherUid(1001L)
                .teachingMode("ONLINE")
                .status("TRIALING")
                .trialEndAt(java.time.LocalDateTime.now().plusDays(1))
                .build());

        ApplyTrialRefundReq req = new ApplyTrialRefundReq();
        req.setReason("试课不合适");
        req.setEvidenceImageUrls(java.util.List.of("https://img.example.com/a.png"));
        req.setEvidenceVideoUrl("https://video.example.com/wechat.mp4");
        req.setEvidenceVideoDurationSeconds(45);

        assertThatThrownBy(() -> service.applyTrialRefund(66L, req, 1001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void submitTrialResultShouldPromoteTeachingWhenPassed() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", mock(RefundRequestMapper.class));
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mock(BrokerageOrderMapper.class));
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", mock(CollaborationProposalMapper.class));
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(service, "roomMapper", mock(RoomMapper.class));
        ReflectionTestUtils.setField(service, "chatService", mock(ChatService.class));

        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .studentUid(2001L)
                .status("TRIAL_WAIT_STUDENT_DECISION")
                .roomId(88L)
                .courseName("试课课程")
                .trialEndAt(java.time.LocalDateTime.of(2026, 4, 23, 21, 0))
                .build());
        when(courseEnrollmentMapper.markTrialPassedWaitingWeeklySchedule(eq(66L), eq("TRIAL_WAIT_STUDENT_DECISION"), any())).thenReturn(1);
        when(courseEnrollmentMapper.selectById(66L)).thenReturn(
                CourseEnrollment.builder()
                        .id(66L)
                        .studentUid(2001L)
                        .status("TRIAL_WAIT_STUDENT_DECISION")
                        .roomId(88L)
                        .courseName("试课课程")
                        .trialEndAt(java.time.LocalDateTime.of(2026, 4, 23, 21, 0))
                        .build(),
                CourseEnrollment.builder()
                        .id(66L)
                        .studentUid(2001L)
                        .status("TRIAL_WAIT_WEEKLY_SCHEDULE")
                        .roomId(88L)
                        .courseName("试课课程")
                        .trialEndAt(java.time.LocalDateTime.of(2026, 4, 23, 21, 0))
                        .weeklyScheduleDeadlineAt(java.time.LocalDateTime.of(2026, 4, 24, 21, 0))
                        .build()
        );

        SubmitTrialResultReq req = new SubmitTrialResultReq();
        req.setResult("PASS");
        service.submitTrialResult(66L, req, 2001L);

        verify(courseEnrollmentMapper).markTrialPassedWaitingWeeklySchedule(eq(66L), eq("TRIAL_WAIT_STUDENT_DECISION"), any());
    }

    @Test
    void submitTrialResultShouldFinishOnlineTrialAndCloseChatWhenFailed() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", mock(RefundRequestMapper.class));
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mock(BrokerageOrderMapper.class));
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", mock(CollaborationProposalMapper.class));
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);

        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .studentUid(2001L)
                .teachingMode("ONLINE")
                .status("TRIAL_WAIT_STUDENT_DECISION")
                .build());
        when(courseEnrollmentMapper.updateStatus(66L, "TRIAL_WAIT_STUDENT_DECISION", "TRIAL_FAILED", null, null, null)).thenReturn(1);

        SubmitTrialResultReq req = new SubmitTrialResultReq();
        req.setResult("FAIL");
        req.setReason("试课后确认不合适");
        service.submitTrialResult(66L, req, 2001L);

        verify(courseEnrollmentMapper).updateStatus(66L, "TRIAL_WAIT_STUDENT_DECISION", "TRIAL_FAILED", null, null, null);
        verify(tutorApplicationMapper).updateChatAccessStatus(501L, "NONE");
        verify(roomMapper).closeRoom(88L);
    }

    @Test
    void confirmWeeklyScheduleSubmittedShouldPromoteTeachingByStudent() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", mock(RefundRequestMapper.class));
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mock(BrokerageOrderMapper.class));
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", mock(CollaborationProposalMapper.class));
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(service, "roomMapper", mock(RoomMapper.class));
        ReflectionTestUtils.setField(service, "chatService", mock(ChatService.class));

        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .studentUid(2001L)
                .status("TRIAL_WAIT_WEEKLY_SCHEDULE")
                .build());
        when(courseEnrollmentMapper.markWeeklyScheduleSubmitted(66L, "TRIAL_WAIT_WEEKLY_SCHEDULE", "周二 19:00-21:00", 1, "200 元/节")).thenReturn(1);

        service.confirmWeeklyScheduleSubmitted(66L, 2001L, "周二 19:00-21:00", 1, 20000L);

        verify(courseEnrollmentMapper).markWeeklyScheduleSubmitted(66L, "TRIAL_WAIT_WEEKLY_SCHEDULE", "周二 19:00-21:00", 1, "200 元/节");
    }

    @Test
    void confirmWeeklyScheduleSubmittedShouldRejectRepeatSubmissionWhenAlreadyTeaching() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", mock(RefundRequestMapper.class));
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mock(BrokerageOrderMapper.class));
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", mock(CollaborationProposalMapper.class));
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", mock(TutorApplicationMapper.class));
        ReflectionTestUtils.setField(service, "roomMapper", mock(RoomMapper.class));
        ReflectionTestUtils.setField(service, "chatService", mock(ChatService.class));

        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .studentUid(2001L)
                .status("TEACHING")
                .build());

        assertThatThrownBy(() -> service.confirmWeeklyScheduleSubmitted(66L, 2001L, "周二 19:00-21:00", 1, 20000L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复提交");
    }

    @Test
    void markTrialCanceledShouldReturnToCommunicatingAndReopenChat() {
        CourseEnrollmentMapper courseEnrollmentMapper = mock(CourseEnrollmentMapper.class);
        TutorApplicationMapper tutorApplicationMapper = mock(TutorApplicationMapper.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        CourseEnrollmentService service = new CourseEnrollmentService();
        ReflectionTestUtils.setField(service, "courseEnrollmentMapper", courseEnrollmentMapper);
        ReflectionTestUtils.setField(service, "refundRequestMapper", mock(RefundRequestMapper.class));
        ReflectionTestUtils.setField(service, "brokerageOrderMapper", mock(BrokerageOrderMapper.class));
        ReflectionTestUtils.setField(service, "collaborationProposalMapper", mock(CollaborationProposalMapper.class));
        ReflectionTestUtils.setField(service, "tutorApplicationMapper", tutorApplicationMapper);
        ReflectionTestUtils.setField(service, "roomMapper", roomMapper);

        when(courseEnrollmentMapper.selectById(66L)).thenReturn(CourseEnrollment.builder()
                .id(66L)
                .applicationId(501L)
                .roomId(88L)
                .teacherUid(1001L)
                .studentUid(2001L)
                .status("TRIALING")
                .build());
        when(courseEnrollmentMapper.updateStatus(66L, "TRIALING", "COMMUNICATING", null, null, null)).thenReturn(1);

        service.markTrialCanceled(66L, 1001L, "临时取消试课");

        verify(courseEnrollmentMapper).updateStatus(66L, "TRIALING", "COMMUNICATING", null, null, null);
        verify(tutorApplicationMapper).updateChatAccessStatus(501L, "CHAT_ENABLED");
        verify(roomMapper).reopenRoom(88L);
    }
}
