package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.integration.feign.LiveClassInternalFeignClient;
import com.ai.tutor.appointment.model.dto.schedule.CreateScheduleEventRequest;
import com.ai.tutor.appointment.model.dto.schedule.InternalTrialEventRequest;
import com.ai.tutor.appointment.model.dto.schedule.SubmitWeeklyScheduleRequest;
import com.ai.tutor.appointment.model.entity.LessonPaymentOrder;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleAvailabilityVO;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleEventVO;
import com.ai.tutor.appointment.service.LessonPaymentOrderService;
import com.ai.tutor.appointment.service.ScheduleService;
import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 课程安排（日历）服务实现。
 *
 * <p>实现策略：复用既有 tutor_appointment 表作为“课程日程”承载，
 * 并通过 IM 系统消息向聊天侧投递“授课申请/状态变更”。</p>
 */
@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_ACCEPTED = 2;
    private static final int STATUS_RESCHEDULE_PENDING = 3;
    private static final int STATUS_CANCELED = 4;
    private static final int STATUS_COMPLETED = 5;
    private static final int STATUS_REJECTED = 6;

    private static final String TRIAL_WEEKLY_DECISION_TODO =
            "TODO(email): 试课通过后正式课表 12h/6h/1h 邮件提醒与 24h 超时失败任务，需要在课程状态事件落到 appointment 后创建/去重。当前聊天侧系统提醒可复用 IM 系统消息。";

    @Resource
    private TutorAppointmentMapper tutorAppointmentMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PositionPostMapper positionPostMapper;

    @Resource
    private ImFacade imFacade;

    @Resource
    private LiveClassInternalFeignClient liveClassInternalFeignClient;

    @Resource
    private LessonPaymentOrderService lessonPaymentOrderService;

    @Override
    public List<ScheduleEventVO> listEvents(Long uid, Long startAtMs, Long endAtMs, boolean includePending) {
        ThrowUtils.throwIf(uid == null || startAtMs == null || endAtMs == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(endAtMs <= startAtMs, ErrorCode.PARAMS_ERROR, "endAt 必须大于 startAt");

        LocalDateTime start = toLocalDateTime(startAtMs);
        LocalDateTime end = toLocalDateTime(endAtMs);

        List<TutorAppointment> list = tutorAppointmentMapper.listByUserAndTimeRange(uid, start, end, includePending);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(a -> toEventVO(a, uid)).collect(Collectors.toList());
    }

    @Override
    public ScheduleAvailabilityVO getDayAvailability(Long uid, Long otherUid, Long dateAtMs) {
        ThrowUtils.throwIf(uid == null || otherUid == null || dateAtMs == null || uid.equals(otherUid), ErrorCode.PARAMS_ERROR);
        User self = userMapper.selectById(uid);
        User other = userMapper.selectById(otherUid);
        ThrowUtils.throwIf(self == null || other == null, ErrorCode.NOT_FOUND_ERROR);

        LocalDate date = Instant.ofEpochMilli(dateAtMs).atZone(BEIJING_ZONE).toLocalDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<TutorAppointment> myEvents = tutorAppointmentMapper.listByUserAndTimeRange(uid, start, end, true);
        List<TutorAppointment> otherEvents = tutorAppointmentMapper.listByUserAndTimeRange(otherUid, start, end, true);
        return ScheduleAvailabilityVO.builder()
                .date(date.toString())
                .timezone(BEIJING_ZONE.getId())
                .myUserId(uid)
                .otherUserId(otherUid)
                .myBusyBlocks(toBusyBlocks(myEvents))
                .otherBusyBlocks(toBusyBlocks(otherEvents))
                .build();
    }

    @Override
    public void assertNoScheduleConflict(Long uid, Long otherUid, Long startAtMs, Long endAtMs) {
        ThrowUtils.throwIf(uid == null || otherUid == null || startAtMs == null || endAtMs == null || uid.equals(otherUid), ErrorCode.PARAMS_ERROR);
        User self = userMapper.selectById(uid);
        User other = userMapper.selectById(otherUid);
        ThrowUtils.throwIf(self == null || other == null, ErrorCode.NOT_FOUND_ERROR);

        LocalDateTime start = toLocalDateTime(startAtMs);
        LocalDateTime end = toLocalDateTime(endAtMs);
        validateTimeRange(start, end);

        List<TutorAppointment> mine = tutorAppointmentMapper.listByUserAndTimeRange(uid, start, end, true);
        List<TutorAppointment> peer = tutorAppointmentMapper.listByUserAndTimeRange(otherUid, start, end, true);
        ThrowUtils.throwIf(hasBlockingConflict(mine, start, end), ErrorCode.OPERATION_ERROR, "与我的日程冲突，请重新选择试课时间");
        ThrowUtils.throwIf(hasBlockingConflict(peer, start, end), ErrorCode.OPERATION_ERROR, "与对方日程冲突，请重新选择试课时间");
    }

    @Override
    public List<ScheduleEventVO> listCourseEvents(Long courseId, Long uid) {
        ThrowUtils.throwIf(courseId == null || uid == null, ErrorCode.PARAMS_ERROR);
        List<TutorAppointment> list = tutorAppointmentMapper.listByCourseId(courseId);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(item -> uid.equals(item.getParentId()) || uid.equals(item.getTutorId()))
                .map(item -> toEventVO(item, uid))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long createAcceptedTrialEvent(InternalTrialEventRequest request) {
        ThrowUtils.throwIf(request == null
                || request.getCourseId() == null
                || request.getRoomId() == null
                || request.getTeacherUid() == null
                || request.getStudentUid() == null
                || request.getStartAt() == null
                || request.getEndAt() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(request.getTeacherUid(), request.getStudentUid()), ErrorCode.PARAMS_ERROR);
        User teacher = userMapper.selectById(request.getTeacherUid());
        User student = userMapper.selectById(request.getStudentUid());
        ThrowUtils.throwIf(teacher == null || student == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!Integer.valueOf(1).equals(teacher.getUserType()), ErrorCode.PARAMS_ERROR, "teacherUid 必须是老师");
        ThrowUtils.throwIf(!Integer.valueOf(2).equals(student.getUserType()) && !Integer.valueOf(3).equals(student.getUserType()), ErrorCode.PARAMS_ERROR, "studentUid 必须是学生/家长");

        TutorAppointment existingTrial = tutorAppointmentMapper.selectAcceptedTrialByCourseId(request.getCourseId());
        if (existingTrial != null && existingTrial.getId() != null) {
            syncLiveSession(existingTrial);
            return existingTrial.getId();
        }

        LocalDateTime start = toLocalDateTime(request.getStartAt());
        LocalDateTime end = toLocalDateTime(request.getEndAt());
        validateTimeRange(start, end);
        int conflicts = tutorAppointmentMapper.countAcceptedConflicts(List.of(request.getTeacherUid(), request.getStudentUid()), start, end);
        ThrowUtils.throwIf(conflicts > 0, ErrorCode.OPERATION_ERROR, "与已有课程冲突，请重新选择试课时间");

        Long subjectId = positionPostMapper.selectFirstEnabledLeafId();
        ThrowUtils.throwIf(subjectId == null, ErrorCode.OPERATION_ERROR, "未找到可用科目，请先初始化科目数据");
        int durationMinutes = (int) Duration.between(start, end).toMinutes();
        TutorAppointment appointment = TutorAppointment.builder()
                .courseId(request.getCourseId())
                .parentId(request.getStudentUid())
                .tutorId(request.getTeacherUid())
                .title(trimTo255(request.getTitle()) == null ? "试课" : trimTo255(request.getTitle()))
                .lessonType("TRIAL")
                .lessonPriceFen(parsePriceFen(request.getLessonPrice()))
                .trialPricePercent(0)
                .payableAmountFen(0L)
                .subjectId(subjectId)
                .startTime(start)
                .durationMinutes(durationMinutes)
                .status(STATUS_ACCEPTED)
                .createdBy(request.getCreatedBy() == null ? request.getTeacherUid() : request.getCreatedBy())
                .roomId(request.getRoomId())
                .remark(trimTo1024(request.getRemark()))
                .build();

        int inserted = tutorAppointmentMapper.insert(appointment);
        ThrowUtils.throwIf(inserted <= 0 || appointment.getId() == null, ErrorCode.OPERATION_ERROR);
        syncLiveSession(appointment);
        sendLessonStatusMsg(appointment, "ACCEPTED", appointment.getCreatedBy());
        return appointment.getId();
    }

    @Override
    @Transactional
    public List<ScheduleEventVO> submitWeeklySchedule(Long courseId, SubmitWeeklyScheduleRequest request, Long uid) {
        ThrowUtils.throwIf(courseId == null || request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getParticipantUserId() == null || uid.equals(request.getParticipantUserId()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getSlots() == null || request.getSlots().isEmpty(), ErrorCode.PARAMS_ERROR, "请选择正式每周课表");

        User self = userMapper.selectById(uid);
        User target = userMapper.selectById(request.getParticipantUserId());
        ThrowUtils.throwIf(self == null || target == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!Integer.valueOf(2).equals(self.getUserType()) && !Integer.valueOf(3).equals(self.getUserType()), ErrorCode.NO_AUTH_ERROR, "正式每周课表只能由学生提交");
        ThrowUtils.throwIf(!Integer.valueOf(1).equals(target.getUserType()), ErrorCode.PARAMS_ERROR, "正式课表接收方必须是老师");
        ThrowUtils.throwIf(hasSubmittedWeeklySchedule(courseId), ErrorCode.OPERATION_ERROR, "正式课表已提交，不能重复提交");

        int duration = validateWeeklySlots(request.getSlots());
        int weeks = request.getWeeks() == null ? 16 : Math.max(1, Math.min(request.getWeeks(), 16));
        Long subjectId = positionPostMapper.selectFirstEnabledLeafId();
        ThrowUtils.throwIf(subjectId == null, ErrorCode.OPERATION_ERROR, "未找到可用科目，请先初始化科目数据");

        List<TutorAppointment> toCreate = expandWeeklyEvents(courseId, uid, request, subjectId, duration, weeks);
        for (TutorAppointment item : toCreate) {
            LocalDateTime start = item.getStartTime();
            LocalDateTime end = start.plusMinutes(item.getDurationMinutes());
            int conflicts = tutorAppointmentMapper.countAcceptedConflicts(List.of(uid, request.getParticipantUserId()), start, end);
            ThrowUtils.throwIf(conflicts > 0, ErrorCode.OPERATION_ERROR, "正式课表与已有课程冲突，请调整后再提交");
        }

        List<ScheduleEventVO> created = new ArrayList<>();
        for (TutorAppointment item : toCreate) {
            int inserted = tutorAppointmentMapper.insert(item);
            ThrowUtils.throwIf(inserted <= 0 || item.getId() == null, ErrorCode.OPERATION_ERROR);
            syncLiveSession(item);
            created.add(toEventVO(item, uid));
        }
        imFacade.confirmWeeklyScheduleSubmitted(
                uid,
                courseId,
                buildWeeklyClassTimeSummary(request.getSlots()),
                request.getSlots().size(),
                request.getLessonPriceFen()
        );
        return created;
    }

    @Override
    @Transactional
    public ScheduleEventVO createEvent(CreateScheduleEventRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getParticipantUserId() == null || uid.equals(request.getParticipantUserId()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getCourseId() == null, ErrorCode.OPERATION_ERROR, "请先发起合作并生成课程后，再从我的课程中安排课节");
        ThrowUtils.throwIf(request.getStartAt() == null || request.getEndAt() == null, ErrorCode.PARAMS_ERROR);

        LocalDateTime start = toLocalDateTime(request.getStartAt());
        LocalDateTime end = toLocalDateTime(request.getEndAt());
        validateTimeRange(start, end);

        User self = userMapper.selectById(uid);
        User target = userMapper.selectById(request.getParticipantUserId());
        ThrowUtils.throwIf(self == null || target == null, ErrorCode.NOT_FOUND_ERROR);

        Long parentId;
        Long tutorId;
        if (Integer.valueOf(2).equals(self.getUserType())) {
            // 家长（学生端）发起 -> 授课对象必须是老师
            ThrowUtils.throwIf(!Integer.valueOf(1).equals(target.getUserType()), ErrorCode.PARAMS_ERROR);
            parentId = uid;
            tutorId = target.getId();
        } else if (Integer.valueOf(1).equals(self.getUserType())) {
            // 老师发起 -> 授课对象必须是家长（学生端）
            ThrowUtils.throwIf(!Integer.valueOf(2).equals(target.getUserType()), ErrorCode.PARAMS_ERROR);
            parentId = target.getId();
            tutorId = uid;
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
            return null;
        }

        // 冲突校验：仅与“已确认”日程冲突（PENDING 不算冲突）
        int conflicts = tutorAppointmentMapper.countAcceptedConflicts(List.of(uid, target.getId()), start, end);
        ThrowUtils.throwIf(conflicts > 0, ErrorCode.OPERATION_ERROR, "与已有课程冲突，请选择其他时间");

        Long subjectId = request.getSubjectId();
        if (subjectId == null) {
            subjectId = positionPostMapper.selectFirstEnabledLeafId();
            ThrowUtils.throwIf(subjectId == null, ErrorCode.OPERATION_ERROR, "未找到可用科目，请先初始化科目数据");
        }
        if (request.getCourseId() != null) {
            LessonPaymentOrder unpaid = lessonPaymentOrderService.findUnpaidByCourseId(request.getCourseId());
            ThrowUtils.throwIf(unpaid != null, ErrorCode.OPERATION_ERROR, "上一节课尚未支付，支付后才能预约下一节课");
        }

        // 关联聊天会话，用于在聊天中投递授课申请卡片
        Long roomId = imFacade.getOrCreateRoomWithUser(uid, target.getId());
        imFacade.assertRoomReadyForScheduling(uid, roomId);

        int durationMinutes = (int) Duration.between(start, end).toMinutes();
        String lessonType = normalizeLessonType(request.getLessonType(), request.getCourseId());
        Long lessonPriceFen = request.getLessonPriceFen();
        Integer trialPricePercent = clampPercent(request.getTrialPricePercent(), 50);
        Long payableAmountFen = computePayableAmountFen(lessonPriceFen, lessonType, trialPricePercent);
        TutorAppointment appointment = TutorAppointment.builder()
                .courseId(request.getCourseId())
                .parentId(parentId)
                .tutorId(tutorId)
                .title(request.getTitle())
                .lessonType(lessonType)
                .lessonPriceFen(lessonPriceFen)
                .trialPricePercent(trialPricePercent)
                .payableAmountFen(payableAmountFen)
                .subjectId(subjectId)
                .startTime(start)
                .durationMinutes(durationMinutes)
                .status(STATUS_PENDING)
                .createdBy(uid)
                .roomId(roomId)
                .remark(request.getDescription())
                .build();

        int inserted = tutorAppointmentMapper.insert(appointment);
        ThrowUtils.throwIf(inserted <= 0 || appointment.getId() == null, ErrorCode.OPERATION_ERROR);

        // 投递授课申请（系统消息），用于聊天侧展示“接收/拒绝”卡片
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "LESSON_REQUEST");
        payload.put("eventId", appointment.getId());
        payload.put("title", appointment.getTitle());
        payload.put("lessonType", lessonType);
        payload.put("payableAmountFen", payableAmountFen);
        payload.put("startAt", request.getStartAt());
        payload.put("endAt", request.getEndAt());
        payload.put("status", "PENDING");
        payload.put("creatorUserId", uid);
        imFacade.sendSystemMessage(uid, roomId, payload);

        return toEventVO(appointment, uid);
    }

    @Override
    @Transactional
    public ScheduleEventVO respond(Long eventId, String action, Long uid) {
        ThrowUtils.throwIf(eventId == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(action == null || action.isBlank(), ErrorCode.PARAMS_ERROR);

        TutorAppointment db = requireParticipant(eventId, uid);
        ThrowUtils.throwIf(!Objects.equals(db.getStatus(), STATUS_PENDING), ErrorCode.OPERATION_ERROR, "当前状态不支持响应");
        ThrowUtils.throwIf(db.getCreatedBy() != null && uid.equals(db.getCreatedBy()), ErrorCode.NO_AUTH_ERROR, "发起方不能响应自己的授课申请");

        String normalized = action.trim().toUpperCase();
        if ("ACCEPT".equals(normalized)) {
            LocalDateTime start = db.getStartTime();
            LocalDateTime end = start.plusMinutes(db.getDurationMinutes() == null ? 60 : db.getDurationMinutes());
            int conflicts = tutorAppointmentMapper.countAcceptedConflicts(List.of(db.getParentId(), db.getTutorId()), start, end);
            ThrowUtils.throwIf(conflicts > 0, ErrorCode.OPERATION_ERROR, "与已有课程冲突，无法接收");

            int updated = tutorAppointmentMapper.acceptIfPending(db.getId());
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "授课申请已被处理");
            sendLessonStatusMsg(db, "ACCEPTED", uid);
            TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
            syncLiveSession(latest);
            return toEventVO(latest, uid);
        }

        if ("REJECT".equals(normalized)) {
            int updated = tutorAppointmentMapper.rejectIfPending(db.getId());
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "授课申请已被处理");
            sendLessonStatusMsg(db, "REJECTED", uid);
            TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
            return toEventVO(latest, uid);
        }

        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "action 仅支持 ACCEPT/REJECT");
        return null;
    }

    @Override
    @Transactional
    public ScheduleEventVO cancel(Long eventId, Long uid, String remark) {
        ThrowUtils.throwIf(eventId == null || uid == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment db = requireParticipant(eventId, uid);
        ThrowUtils.throwIf(db.getStatus() == null, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(Objects.equals(db.getStatus(), STATUS_CANCELED), ErrorCode.OPERATION_ERROR, "已取消");
        ThrowUtils.throwIf(Objects.equals(db.getStatus(), STATUS_REJECTED), ErrorCode.OPERATION_ERROR, "已拒绝");

        // 待确认：仅发起方可取消；已确认：任一方可取消
        if (Objects.equals(db.getStatus(), STATUS_PENDING)) {
            ThrowUtils.throwIf(db.getCreatedBy() == null || !uid.equals(db.getCreatedBy()), ErrorCode.NO_AUTH_ERROR, "仅发起方可取消申请");
        }

        TutorAppointment toUpdate = TutorAppointment.builder()
                .id(db.getId())
                .status(STATUS_CANCELED)
                .cancelBy(uid)
                .proposedStartTime(db.getProposedStartTime())
                .proposedBy(db.getProposedBy())
                .remark(remark)
                .build();
        int updated = tutorAppointmentMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        sendLessonStatusMsg(db, "CANCELED", uid);
        if ("TRIAL".equalsIgnoreCase(db.getLessonType()) && db.getCourseId() != null && db.getStartTime() != null) {
            ThrowUtils.throwIf(!LocalDateTime.now(BEIJING_ZONE).isBefore(db.getStartTime()), ErrorCode.OPERATION_ERROR, "试课开始后不能取消");
            imFacade.markTrialCanceled(uid, db.getCourseId(), remark);
        }
        TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
        return toEventVO(latest, uid);
    }

    private void sendLessonStatusMsg(TutorAppointment appointment, String status, Long actorUid) {
        if (appointment == null || appointment.getRoomId() == null || appointment.getStartTime() == null) {
            return;
        }
        long startAt = toEpochMillis(appointment.getStartTime());
        long endAt = toEpochMillis(appointment.getStartTime().plusMinutes(appointment.getDurationMinutes() == null ? 60 : appointment.getDurationMinutes()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "LESSON_STATUS");
        payload.put("eventId", appointment.getId());
        payload.put("title", appointment.getTitle());
        payload.put("startAt", startAt);
        payload.put("endAt", endAt);
        payload.put("status", status);
        payload.put("actorUserId", actorUid);
        try {
            imFacade.sendSystemMessage(actorUid, appointment.getRoomId(), payload);
        } catch (Exception ex) {
            log.warn("send lesson status message failed, appointmentId={}, roomId={}, status={}",
                    appointment.getId(),
                    appointment.getRoomId(),
                    status,
                    ex);
        }
    }

    private void syncLiveSession(TutorAppointment appointment) {
        if (appointment == null || appointment.getId() == null || appointment.getStartTime() == null) {
            return;
        }
        int minutes = appointment.getDurationMinutes() == null ? 60 : appointment.getDurationMinutes();
        LiveClassInternalFeignClient.SyncCourseSessionRequest request = new LiveClassInternalFeignClient.SyncCourseSessionRequest();
        request.setCourseId(appointment.getCourseId());
        request.setScheduleEventId(appointment.getId());
        request.setRoomId(appointment.getRoomId());
        request.setTeacherUid(appointment.getTutorId());
        request.setStudentUid(appointment.getParentId());
        request.setTitle(appointment.getTitle());
        request.setScheduledStartAt(appointment.getStartTime());
        request.setScheduledEndAt(appointment.getStartTime().plusMinutes(minutes));
        request.setRecordPolicy("OFF");
        request.setAiPolicy("OFF");
        try {
            liveClassInternalFeignClient.syncFromCourse(request);
        } catch (Exception ignored) {
            // 课堂域同步失败不影响约课主链路，后续可通过重试任务补齐。
        }
    }

    private TutorAppointment requireParticipant(Long id, Long uid) {
        TutorAppointment db = tutorAppointmentMapper.selectById(id);
        ThrowUtils.throwIf(db == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(db.getParentId()) && !uid.equals(db.getTutorId()), ErrorCode.NO_AUTH_ERROR);
        return db;
    }

    private ScheduleEventVO toEventVO(TutorAppointment a, Long viewerUid) {
        if (a == null) {
            return null;
        }
        Long otherUid = resolveOtherUid(a, viewerUid);
        User other = otherUid == null ? null : userMapper.selectById(otherUid);
        UserSimpleVO otherVo = other == null ? null : UserSimpleVO.builder()
                .id(other.getId())
                .name(other.getName())
                .avatar(other.getAvatar())
                .userType(other.getUserType())
                .build();

        LocalDateTime start = a.getStartTime();
        int minutes = a.getDurationMinutes() == null ? 60 : a.getDurationMinutes();
        LocalDateTime end = start == null ? null : start.plusMinutes(minutes);
        LocalDateTime proposedStart = a.getProposedStartTime();
        LocalDateTime proposedEnd = proposedStart == null ? null : proposedStart.plusMinutes(minutes);
        LessonPaymentOrder paymentOrder = lessonPaymentOrderService.getByLessonId(a.getId());

        return ScheduleEventVO.builder()
                .id(a.getId())
                .courseId(a.getCourseId())
                .title(a.getTitle() == null || a.getTitle().isBlank() ? "课程" : a.getTitle())
                .lessonType(normalizeLessonType(a.getLessonType(), null))
                .lessonPriceFen(a.getLessonPriceFen())
                .trialPricePercent(a.getTrialPricePercent())
                .payableAmountFen(a.getPayableAmountFen())
                .paymentStatus(paymentOrder == null ? "UNBILLED" : paymentOrder.getStatus())
                .lessonPaymentOrderId(paymentOrder == null ? null : paymentOrder.getId())
                .platformFeeRate(paymentOrder == null ? 10 : paymentOrder.getPlatformFeeRate())
                .platformFeeAmountFen(paymentOrder == null ? computePlatformFee(a.getPayableAmountFen()) : paymentOrder.getPlatformFeeAmountFen())
                .teacherIncomeAmountFen(paymentOrder == null ? computeTeacherIncome(a.getPayableAmountFen()) : paymentOrder.getTeacherIncomeAmountFen())
                .description(a.getRemark())
                .startAt(start == null ? null : toEpochMillis(start))
                .endAt(end == null ? null : toEpochMillis(end))
                .status(mapStatus(a.getStatus()))
                .creatorUserId(a.getCreatedBy())
                .participant(otherVo)
                .chatRoomId(a.getRoomId())
                .durationMinutes(minutes)
                .proposedStartAt(proposedStart == null ? null : toEpochMillis(proposedStart))
                .proposedEndAt(proposedEnd == null ? null : toEpochMillis(proposedEnd))
                .proposedBy(a.getProposedBy())
                .cancelBy(a.getCancelBy())
                .build();
    }

    private static String mapStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        if (status == STATUS_PENDING) return "PENDING";
        if (status == STATUS_ACCEPTED) return "ACCEPTED";
        if (status == STATUS_RESCHEDULE_PENDING) return "RESCHEDULE_PENDING";
        if (status == STATUS_CANCELED) return "CANCELED";
        if (status == STATUS_COMPLETED) return "COMPLETED";
        if (status == STATUS_REJECTED) return "REJECTED";
        return "UNKNOWN";
    }

    private static Long resolveOtherUid(TutorAppointment a, Long viewerUid) {
        if (a == null || viewerUid == null) {
            return null;
        }
        if (viewerUid.equals(a.getParentId())) {
            return a.getTutorId();
        }
        if (viewerUid.equals(a.getTutorId())) {
            return a.getParentId();
        }
        return null;
    }

    private static List<ScheduleAvailabilityVO.BusyBlockVO> toBusyBlocks(List<TutorAppointment> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .map(item -> {
                    LocalDateTime start = item.getStartTime();
                    int minutes = item.getDurationMinutes() == null ? 60 : item.getDurationMinutes();
                    LocalDateTime end = start == null ? null : start.plusMinutes(minutes);
                    return ScheduleAvailabilityVO.BusyBlockVO.builder()
                            .eventId(item.getId())
                            .courseId(item.getCourseId())
                            .title(item.getTitle())
                            .lessonType(item.getLessonType())
                            .startAt(start == null ? null : toEpochMillis(start))
                            .endAt(end == null ? null : toEpochMillis(end))
                            .status(mapStatus(item.getStatus()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static boolean hasBlockingConflict(List<TutorAppointment> list, LocalDateTime start, LocalDateTime end) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        return list.stream().anyMatch(item -> {
            if (item == null || item.getStartTime() == null) {
                return false;
            }
            Integer status = item.getStatus();
            if (status == null || status == STATUS_CANCELED || status == STATUS_REJECTED) {
                return false;
            }
            LocalDateTime itemEnd = item.getStartTime().plusMinutes(item.getDurationMinutes() == null ? 60 : item.getDurationMinutes());
            return item.getStartTime().isBefore(end) && itemEnd.isAfter(start);
        });
    }

    private static int validateWeeklySlots(List<SubmitWeeklyScheduleRequest.WeeklySlot> slots) {
        Integer duration = null;
        for (SubmitWeeklyScheduleRequest.WeeklySlot slot : slots) {
            ThrowUtils.throwIf(slot == null || slot.getDayOfWeek() == null || slot.getStartMinute() == null || slot.getEndMinute() == null, ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(slot.getDayOfWeek() < 1 || slot.getDayOfWeek() > 7, ErrorCode.PARAMS_ERROR, "星期值必须为 1-7");
            ThrowUtils.throwIf(slot.getStartMinute() < 0 || slot.getStartMinute() >= 24 * 60, ErrorCode.PARAMS_ERROR, "开始时间无效");
            ThrowUtils.throwIf(slot.getEndMinute() <= slot.getStartMinute() || slot.getEndMinute() > 24 * 60, ErrorCode.PARAMS_ERROR, "结束时间无效");
            int current = slot.getEndMinute() - slot.getStartMinute();
            ThrowUtils.throwIf(current < 15 || current > 8 * 60, ErrorCode.PARAMS_ERROR, "单节时长需在 15 分钟到 8 小时之间");
            if (duration == null) {
                duration = current;
            } else {
                ThrowUtils.throwIf(duration != current, ErrorCode.PARAMS_ERROR, "所有固定课节时长必须一致");
            }
        }
        return duration == null ? 0 : duration;
    }

    private static String buildWeeklyClassTimeSummary(List<SubmitWeeklyScheduleRequest.WeeklySlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return null;
        }
        Map<Integer, String> dayNames = Map.of(
                1, "周一",
                2, "周二",
                3, "周三",
                4, "周四",
                5, "周五",
                6, "周六",
                7, "周日"
        );
        return slots.stream()
                .sorted((a, b) -> {
                    int d = Integer.compare(a.getDayOfWeek() == null ? 0 : a.getDayOfWeek(), b.getDayOfWeek() == null ? 0 : b.getDayOfWeek());
                    if (d != 0) {
                        return d;
                    }
                    return Integer.compare(a.getStartMinute() == null ? 0 : a.getStartMinute(), b.getStartMinute() == null ? 0 : b.getStartMinute());
                })
                .map(slot -> (dayNames.getOrDefault(slot.getDayOfWeek(), "周" + slot.getDayOfWeek()))
                        + " "
                        + formatMinute(slot.getStartMinute())
                        + "-"
                        + formatMinute(slot.getEndMinute()))
                .collect(Collectors.joining("；"));
    }

    private static String formatMinute(Integer minute) {
        int safe = minute == null ? 0 : Math.max(0, Math.min(minute, 24 * 60));
        int h = safe / 60;
        int m = safe % 60;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", h, m);
    }

    private List<TutorAppointment> expandWeeklyEvents(Long courseId, Long uid, SubmitWeeklyScheduleRequest request, Long subjectId, int duration, int weeks) {
        LocalDate base = LocalDate.now(BEIJING_ZONE);
        List<TutorAppointment> out = new ArrayList<>();
        for (int w = 0; w < weeks; w++) {
            for (SubmitWeeklyScheduleRequest.WeeklySlot slot : request.getSlots()) {
                LocalDate date = base.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(slot.getDayOfWeek()))).plusWeeks(w);
                LocalTime time = LocalTime.of(slot.getStartMinute() / 60, slot.getStartMinute() % 60);
                LocalDateTime start = LocalDateTime.of(date, time);
                if (start.isBefore(LocalDateTime.now(BEIJING_ZONE))) {
                    continue;
                }
                out.add(TutorAppointment.builder()
                        .courseId(courseId)
                        .parentId(uid)
                        .tutorId(request.getParticipantUserId())
                        .title(trimTo255(request.getTitle()) == null ? "正式每周课" : trimTo255(request.getTitle()))
                        .lessonType("NORMAL")
                        .lessonPriceFen(request.getLessonPriceFen())
                        .trialPricePercent(100)
                        .payableAmountFen(request.getLessonPriceFen())
                        .subjectId(subjectId)
                        .startTime(start)
                        .durationMinutes(duration)
                        .status(STATUS_ACCEPTED)
                        .createdBy(uid)
                        .roomId(request.getRoomId())
                        .remark(trimTo1024(request.getDescription()))
                        .build());
            }
        }
        ThrowUtils.throwIf(out.isEmpty(), ErrorCode.PARAMS_ERROR, "正式课表至少需要生成一节未来课程");
        return out;
    }

    private boolean hasSubmittedWeeklySchedule(Long courseId) {
        List<TutorAppointment> existing = tutorAppointmentMapper.listByCourseId(courseId);
        if (existing == null || existing.isEmpty()) {
            return false;
        }
        return existing.stream().anyMatch(item -> item != null && "NORMAL".equalsIgnoreCase(item.getLessonType()));
    }

    private static void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        ThrowUtils.throwIf(start == null || end == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!end.isAfter(start), ErrorCode.PARAMS_ERROR, "结束时间必须晚于开始时间");

        long minutes = Duration.between(start, end).toMinutes();
        ThrowUtils.throwIf(minutes < 15, ErrorCode.PARAMS_ERROR, "最短时长为 15 分钟");
        ThrowUtils.throwIf(minutes > 8 * 60, ErrorCode.PARAMS_ERROR, "最长时长为 8 小时");

        LocalDate d1 = start.toLocalDate();
        LocalDate d2 = end.toLocalDate();
        ThrowUtils.throwIf(!Objects.equals(d1, d2), ErrorCode.PARAMS_ERROR, "V1 不支持跨天日程");
    }

    private static Long parsePriceFen(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.replace("元/小时", "")
                .replace("元", "")
                .replace("/小时", "")
                .trim();
        try {
            double value = Double.parseDouble(normalized);
            return value > 0 ? Math.round(value * 100) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String trimTo255(String s) {
        String v = trimTo1024(s);
        if (v == null || v.length() <= 255) {
            return v;
        }
        return v.substring(0, 255);
    }

    private static String trimTo1024(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        return v.length() <= 1024 ? v : v.substring(0, 1024);
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), BEIJING_ZONE);
    }

    private String normalizeLessonType(String raw, Long courseId) {
        if (raw != null && !raw.isBlank()) {
            String normalized = raw.trim().toUpperCase();
            ThrowUtils.throwIf(!"TRIAL".equals(normalized) && !"NORMAL".equals(normalized), ErrorCode.PARAMS_ERROR, "课节类型仅支持 TRIAL/NORMAL");
            return normalized;
        }
        if (courseId != null) {
            List<TutorAppointment> existing = tutorAppointmentMapper.listByCourseId(courseId);
            if (existing == null || existing.isEmpty()) {
                return "TRIAL";
            }
        }
        return "NORMAL";
    }

    private static Integer clampPercent(Integer value, int defaultValue) {
        int v = value == null ? defaultValue : value;
        if (v < 1) return 1;
        if (v > 100) return 100;
        return v;
    }

    private static Long computePayableAmountFen(Long lessonPriceFen, String lessonType, Integer trialPricePercent) {
        if (lessonPriceFen == null || lessonPriceFen <= 0) {
            return null;
        }
        if ("TRIAL".equalsIgnoreCase(lessonType)) {
            // 中文注释：线上试课默认按半节课收费，可由发起试课时的 trialPricePercent 调整。
            return lessonPriceFen * clampPercent(trialPricePercent, 50) / 100;
        }
        return lessonPriceFen;
    }

    private static Long computePlatformFee(Long amountFen) {
        if (amountFen == null || amountFen <= 0) {
            return null;
        }
        return amountFen * 10 / 100;
    }

    private static Long computeTeacherIncome(Long amountFen) {
        Long platformFee = computePlatformFee(amountFen);
        if (amountFen == null || platformFee == null) {
            return null;
        }
        return Math.max(0L, amountFen - platformFee);
    }

    private static long toEpochMillis(LocalDateTime time) {
        return time.atZone(BEIJING_ZONE).toInstant().toEpochMilli();
    }
}
