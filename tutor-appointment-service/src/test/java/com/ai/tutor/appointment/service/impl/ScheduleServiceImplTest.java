package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.integration.feign.LiveClassInternalFeignClient;
import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.schedule.CreateScheduleEventRequest;
import com.ai.tutor.appointment.model.dto.schedule.SubmitWeeklyScheduleRequest;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleAvailabilityVO;
import com.ai.tutor.appointment.service.LessonPaymentOrderService;
import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceImplTest {

    @Test
    void createEventShouldRequireCourseId() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);

        ScheduleServiceImpl service = new ScheduleServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);

        CreateScheduleEventRequest req = new CreateScheduleEventRequest();
        req.setTitle("线上试课");
        req.setParticipantUserId(2001L);
        req.setStartAt(System.currentTimeMillis() + 3_600_000L);
        req.setEndAt(System.currentTimeMillis() + 7_200_000L);

        assertThatThrownBy(() -> service.createEvent(req, 1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先发起合作并生成课程后");
    }

    @Test
    void createEventShouldCheckRoomSchedulingReadinessBeforeInsert() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PositionPostMapper positionPostMapper = mock(PositionPostMapper.class);
        ImFacade imFacade = mock(ImFacade.class);
        LiveClassInternalFeignClient liveClassInternalFeignClient = mock(LiveClassInternalFeignClient.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);

        ScheduleServiceImpl service = new ScheduleServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "positionPostMapper", positionPostMapper);
        ReflectionTestUtils.setField(service, "imFacade", imFacade);
        ReflectionTestUtils.setField(service, "liveClassInternalFeignClient", liveClassInternalFeignClient);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);

        when(userMapper.selectById(1001L)).thenReturn(User.builder().id(1001L).userType(1).name("老师").build());
        when(userMapper.selectById(2001L)).thenReturn(User.builder().id(2001L).userType(2).name("学生").build());
        when(tutorAppointmentMapper.countAcceptedConflicts(any(), any(), any())).thenReturn(0);
        when(positionPostMapper.selectFirstEnabledLeafId()).thenReturn(201L);
        when(imFacade.getOrCreateRoomWithUser(1001L, 2001L)).thenReturn(88L);
        doNothing().when(imFacade).assertRoomReadyForScheduling(1001L, 88L);
        when(imFacade.sendSystemMessage(eq(1001L), eq(88L), any())).thenReturn(9001L);
        when(tutorAppointmentMapper.insert(any())).thenAnswer(invocation -> {
            TutorAppointment appointment = invocation.getArgument(0);
            appointment.setId(66L);
            return 1;
        });

        CreateScheduleEventRequest req = new CreateScheduleEventRequest();
        req.setCourseId(66L);
        req.setTitle("线上试课");
        req.setParticipantUserId(2001L);
        req.setStartAt(System.currentTimeMillis() + 3_600_000L);
        req.setEndAt(System.currentTimeMillis() + 7_200_000L);
        req.setDescription("先试课");

        service.createEvent(req, 1001L);

        verify(imFacade).assertRoomReadyForScheduling(1001L, 88L);
        ArgumentCaptor<TutorAppointment> captor = ArgumentCaptor.forClass(TutorAppointment.class);
        verify(tutorAppointmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getRoomId()).isEqualTo(88L);
    }

    @Test
    void getDayAvailabilityShouldReturnBothUsersBusyBlocks() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);

        ScheduleServiceImpl service = new ScheduleServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);

        when(userMapper.selectById(1001L)).thenReturn(User.builder().id(1001L).userType(1).name("老师").build());
        when(userMapper.selectById(2001L)).thenReturn(User.builder().id(2001L).userType(2).name("学生").build());
        when(tutorAppointmentMapper.listByUserAndTimeRange(eq(1001L), any(), any(), eq(true))).thenReturn(List.of(TutorAppointment.builder()
                .id(10L)
                .title("老师已有课")
                .lessonType("NORMAL")
                .startTime(LocalDateTime.of(2026, 4, 21, 18, 0))
                .durationMinutes(120)
                .status(2)
                .build()));
        when(tutorAppointmentMapper.listByUserAndTimeRange(eq(2001L), any(), any(), eq(true))).thenReturn(List.of(TutorAppointment.builder()
                .id(11L)
                .title("学生已有课")
                .lessonType("TRIAL")
                .startTime(LocalDateTime.of(2026, 4, 21, 20, 0))
                .durationMinutes(60)
                .status(1)
                .build()));

        long dateAt = LocalDateTime.of(2026, 4, 21, 0, 0)
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toInstant()
                .toEpochMilli();

        ScheduleAvailabilityVO vo = service.getDayAvailability(1001L, 2001L, dateAt);

        assertThat(vo.getTimezone()).isEqualTo("Asia/Shanghai");
        assertThat(vo.getMyBusyBlocks()).hasSize(1);
        assertThat(vo.getOtherBusyBlocks()).hasSize(1);
        assertThat(vo.getMyBusyBlocks().get(0).getTitle()).isEqualTo("老师已有课");
        assertThat(vo.getOtherBusyBlocks().get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void assertNoScheduleConflictShouldRejectPendingOrAcceptedBusyBlocks() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);

        ScheduleServiceImpl service = new ScheduleServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);

        when(userMapper.selectById(1001L)).thenReturn(User.builder().id(1001L).userType(1).name("老师").build());
        when(userMapper.selectById(2001L)).thenReturn(User.builder().id(2001L).userType(2).name("学生").build());
        when(tutorAppointmentMapper.listByUserAndTimeRange(eq(1001L), any(), any(), eq(true))).thenReturn(List.of());
        when(tutorAppointmentMapper.listByUserAndTimeRange(eq(2001L), any(), any(), eq(true))).thenReturn(List.of(TutorAppointment.builder()
                .id(11L)
                .title("待确认试课")
                .startTime(LocalDateTime.of(2026, 4, 21, 19, 0))
                .durationMinutes(120)
                .status(1)
                .build()));

        long startAt = LocalDateTime.of(2026, 4, 21, 20, 0)
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toInstant()
                .toEpochMilli();
        long endAt = LocalDateTime.of(2026, 4, 21, 22, 0)
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toInstant()
                .toEpochMilli();

        assertThatThrownBy(() -> service.assertNoScheduleConflict(1001L, 2001L, startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("与对方日程冲突");
    }

    @Test
    void submitWeeklyScheduleShouldRejectRepeatSubmissionWhenNormalLessonAlreadyExists() {
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PositionPostMapper positionPostMapper = mock(PositionPostMapper.class);
        ImFacade imFacade = mock(ImFacade.class);
        LiveClassInternalFeignClient liveClassInternalFeignClient = mock(LiveClassInternalFeignClient.class);
        LessonPaymentOrderService lessonPaymentOrderService = mock(LessonPaymentOrderService.class);

        ScheduleServiceImpl service = new ScheduleServiceImpl();
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "positionPostMapper", positionPostMapper);
        ReflectionTestUtils.setField(service, "imFacade", imFacade);
        ReflectionTestUtils.setField(service, "liveClassInternalFeignClient", liveClassInternalFeignClient);
        ReflectionTestUtils.setField(service, "lessonPaymentOrderService", lessonPaymentOrderService);

        when(userMapper.selectById(2001L)).thenReturn(User.builder().id(2001L).userType(2).name("学生").build());
        when(userMapper.selectById(1001L)).thenReturn(User.builder().id(1001L).userType(1).name("老师").build());
        when(tutorAppointmentMapper.listByCourseId(66L)).thenReturn(List.of(
                TutorAppointment.builder().id(1L).courseId(66L).lessonType("TRIAL").build(),
                TutorAppointment.builder().id(2L).courseId(66L).lessonType("NORMAL").build()
        ));

        SubmitWeeklyScheduleRequest req = new SubmitWeeklyScheduleRequest();
        req.setParticipantUserId(1001L);
        req.setTitle("正式每周课");
        req.setLessonPriceFen(20000L);
        SubmitWeeklyScheduleRequest.WeeklySlot slot = new SubmitWeeklyScheduleRequest.WeeklySlot();
        slot.setDayOfWeek(2);
        slot.setStartMinute(19 * 60);
        slot.setEndMinute(21 * 60);
        req.setSlots(List.of(slot));

        assertThatThrownBy(() -> service.submitWeeklySchedule(66L, req, 2001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正式课表已提交");
    }
}
