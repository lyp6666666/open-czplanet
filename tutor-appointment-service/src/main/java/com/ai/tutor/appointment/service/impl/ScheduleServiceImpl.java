package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.integration.feign.LiveClassInternalFeignClient;
import com.ai.tutor.appointment.model.dto.schedule.CreateScheduleEventRequest;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleEventVO;
import com.ai.tutor.appointment.service.ScheduleService;
import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
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
public class ScheduleServiceImpl implements ScheduleService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_ACCEPTED = 2;
    private static final int STATUS_RESCHEDULE_PENDING = 3;
    private static final int STATUS_CANCELED = 4;
    private static final int STATUS_COMPLETED = 5;
    private static final int STATUS_REJECTED = 6;

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
    public ScheduleEventVO createEvent(CreateScheduleEventRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getParticipantUserId() == null || uid.equals(request.getParticipantUserId()), ErrorCode.PARAMS_ERROR);
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

        // 关联聊天会话，用于在聊天中投递授课申请卡片
        Long roomId = imFacade.getOrCreateRoomWithUser(uid, target.getId());

        int durationMinutes = (int) Duration.between(start, end).toMinutes();
        TutorAppointment appointment = TutorAppointment.builder()
                .courseId(request.getCourseId())
                .parentId(parentId)
                .tutorId(tutorId)
                .title(request.getTitle())
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
                .remark(remark)
                .build();
        int updated = tutorAppointmentMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        sendLessonStatusMsg(db, "CANCELED", uid);
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
        imFacade.sendSystemMessage(actorUid, appointment.getRoomId(), payload);
    }

    private void syncLiveSession(TutorAppointment appointment) {
        if (appointment == null || appointment.getId() == null || appointment.getStartTime() == null) {
            return;
        }
        int minutes = appointment.getDurationMinutes() == null ? 60 : appointment.getDurationMinutes();
        LiveClassInternalFeignClient.SyncCourseSessionRequest request = new LiveClassInternalFeignClient.SyncCourseSessionRequest();
        request.setCourseId(appointment.getId());
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

        return ScheduleEventVO.builder()
                .id(a.getId())
                .courseId(a.getCourseId())
                .title(a.getTitle() == null || a.getTitle().isBlank() ? "课程" : a.getTitle())
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

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private static long toEpochMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
