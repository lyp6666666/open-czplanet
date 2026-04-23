package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.appointment.RescheduleAppointmentRequest;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.service.EmailNotificationService;
import com.ai.tutor.appointment.service.LessonPaymentOrderService;
import com.ai.tutor.common.integration.AppointmentEventPublisher;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TutorAppointmentServiceImplTest {

    @Test
    void rescheduleShouldRecordCreatedMetricAfterStatusUpdate() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        StudentJobPostingMapper studentJobPostingMapper = mock(StudentJobPostingMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        AppointmentEventPublisher appointmentEventPublisher = mock(AppointmentEventPublisher.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);
        EmailNotificationService emailNotificationService = mock(EmailNotificationService.class);
        BizKpiMetrics bizKpiMetrics = mock(BizKpiMetrics.class);

        TutorAppointmentServiceImpl service = new TutorAppointmentServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "studentJobPostingMapper", studentJobPostingMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "appointmentEventPublisher", appointmentEventPublisher);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);
        ReflectionTestUtils.setField(service, "emailNotificationService", emailNotificationService);
        ReflectionTestUtils.setField(service, "bizKpiMetrics", bizKpiMetrics);

        TutorAppointment db = TutorAppointment.builder()
                .id(101L)
                .parentId(2001L)
                .tutorId(1001L)
                .status(2)
                .startTime(LocalDateTime.of(2026, 4, 23, 18, 0))
                .durationMinutes(60)
                .createdBy(2001L)
                .build();
        when(tutorAppointmentMapper.selectById(101L)).thenReturn(db);
        when(tutorAppointmentMapper.countAcceptedConflictsExcept(any(), any(), any(), anyLong())).thenReturn(0);
        when(tutorAppointmentMapper.updateById(any(TutorAppointment.class))).thenReturn(1);
        doNothing().when(emailNotificationService).cancelLessonStartTasks(101L, "lesson reschedule requested");

        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest();
        req.setProposedStartTime(LocalDateTime.of(2026, 4, 24, 19, 0));
        req.setDurationMinutes(90);
        req.setRemark("学校活动冲突，想改到周五晚上");

        service.reschedule(101L, req, 2001L);

        verify(bizKpiMetrics).incTrialRescheduleCreated();
    }

    @Test
    void confirmRescheduleShouldRecordAcceptedDecisionMetric() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        StudentJobPostingMapper studentJobPostingMapper = mock(StudentJobPostingMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        AppointmentEventPublisher appointmentEventPublisher = mock(AppointmentEventPublisher.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);
        EmailNotificationService emailNotificationService = mock(EmailNotificationService.class);
        BizKpiMetrics bizKpiMetrics = mock(BizKpiMetrics.class);

        TutorAppointmentServiceImpl service = new TutorAppointmentServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "studentJobPostingMapper", studentJobPostingMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "appointmentEventPublisher", appointmentEventPublisher);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);
        ReflectionTestUtils.setField(service, "emailNotificationService", emailNotificationService);
        ReflectionTestUtils.setField(service, "bizKpiMetrics", bizKpiMetrics);

        TutorAppointment pendingReschedule = TutorAppointment.builder()
                .id(102L)
                .parentId(2001L)
                .tutorId(1001L)
                .status(3)
                .proposedBy(2001L)
                .proposedStartTime(LocalDateTime.of(2026, 4, 24, 19, 0))
                .durationMinutes(90)
                .build();
        TutorAppointment latest = TutorAppointment.builder()
                .id(102L)
                .parentId(2001L)
                .tutorId(1001L)
                .status(2)
                .startTime(LocalDateTime.of(2026, 4, 24, 19, 0))
                .durationMinutes(90)
                .build();
        when(tutorAppointmentMapper.selectById(102L)).thenReturn(pendingReschedule, latest);
        when(tutorAppointmentMapper.countAcceptedConflictsExcept(any(List.class), any(), any(), anyLong())).thenReturn(0);
        when(tutorAppointmentMapper.confirmReschedule(102L)).thenReturn(1);
        doNothing().when(emailNotificationService).cancelLessonStartTasks(102L, "lesson rescheduled");
        doNothing().when(emailNotificationService).createLessonStartTasks(latest);

        service.confirmReschedule(102L, 1001L);

        verify(bizKpiMetrics).incTrialRescheduleDecision("accepted");
    }
}
