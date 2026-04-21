package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.appointment.CreateAppointmentRequest;
import com.ai.tutor.appointment.model.dto.appointment.RescheduleAppointmentRequest;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.common.integration.AppointmentEventPublisher;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TutorAppointmentServiceImpl implements com.ai.tutor.appointment.service.TutorAppointmentService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_ACCEPTED = 2;
    private static final int STATUS_RESCHEDULE_PENDING = 3;
    private static final int STATUS_CANCELED = 4;
    private static final int STATUS_COMPLETED = 5;

    @Resource
    private TutorAppointmentMapper tutorAppointmentMapper;

    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AppointmentEventPublisher appointmentEventPublisher;

    @Resource
    private com.ai.tutor.appointment.service.LessonPaymentOrderService lessonPaymentOrderService;

    @Resource
    private com.ai.tutor.appointment.service.EmailNotificationService emailNotificationService;

    @Override
    public Long create(CreateAppointmentRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTargetUid() == null || uid.equals(request.getTargetUid()), ErrorCode.PARAMS_ERROR);

        User self = userMapper.selectById(uid);
        User target = userMapper.selectById(request.getTargetUid());
        ThrowUtils.throwIf(self == null || target == null, ErrorCode.NOT_FOUND_ERROR);

        Long parentId;
        Long tutorId;
        if (Integer.valueOf(2).equals(self.getUserType()) || Integer.valueOf(3).equals(self.getUserType())) {
            ThrowUtils.throwIf(!Integer.valueOf(1).equals(target.getUserType()), ErrorCode.PARAMS_ERROR);
            parentId = uid;
            tutorId = target.getId();
        } else if (Integer.valueOf(1).equals(self.getUserType())) {
            ThrowUtils.throwIf(!Integer.valueOf(2).equals(target.getUserType()) && !Integer.valueOf(3).equals(target.getUserType()), ErrorCode.PARAMS_ERROR);
            parentId = target.getId();
            tutorId = uid;
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
            return null;
        }

        ThrowUtils.throwIf(request.getSubjectId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getStartTime() == null, ErrorCode.PARAMS_ERROR);

        TutorAppointment appointment = TutorAppointment.builder()
                .parentId(parentId)
                .tutorId(tutorId)
                .parentJobPostingId(request.getParentJobPostingId())
                .tutorJobPostingId(request.getTutorJobPostingId())
                .subjectId(request.getSubjectId())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .startTime(request.getStartTime())
                .durationMinutes(request.getDurationMinutes())
                .status(STATUS_PENDING)
                .createdBy(uid)
                .remark(request.getRemark())
                .build();

        int inserted = tutorAppointmentMapper.insert(appointment);
        ThrowUtils.throwIf(inserted <= 0 || appointment.getId() == null, ErrorCode.OPERATION_ERROR);
        return appointment.getId();
    }

    @Override
    public void accept(Long id, Long uid) {
        TutorAppointment db = requireParticipant(id, uid);
        ThrowUtils.throwIf(db.getStatus() == null, ErrorCode.OPERATION_ERROR);
        if (db.getStatus() == STATUS_ACCEPTED) {
            return;
        }
        ThrowUtils.throwIf(db.getStatus() != STATUS_PENDING, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(db.getCreatedBy() != null && uid.equals(db.getCreatedBy()), ErrorCode.NO_AUTH_ERROR);

        int updated = tutorAppointmentMapper.acceptIfPending(db.getId());
        if (updated > 0) {
            appointmentEventPublisher.publishAccepted(db.getId(), db.getParentId(), db.getTutorId());
            TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
            emailNotificationService.createLessonStartTasks(latest == null ? db : latest);
            return;
        }

        TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
        ThrowUtils.throwIf(latest == null || latest.getStatus() == null, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(latest.getStatus() != STATUS_ACCEPTED, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void reschedule(Long id, RescheduleAppointmentRequest request, Long uid) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment db = requireParticipant(id, uid);
        ThrowUtils.throwIf(db.getStatus() == null || db.getStatus() != STATUS_ACCEPTED, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(request.getProposedStartTime() == null, ErrorCode.PARAMS_ERROR);
        int duration = request.getDurationMinutes() == null ? (db.getDurationMinutes() == null ? 60 : db.getDurationMinutes()) : request.getDurationMinutes();
        LocalDateTime proposedEnd = request.getProposedStartTime().plusMinutes(duration);
        int conflicts = tutorAppointmentMapper.countAcceptedConflictsExcept(
                List.of(db.getParentId(), db.getTutorId()),
                request.getProposedStartTime(),
                proposedEnd,
                db.getId()
        );
        ThrowUtils.throwIf(conflicts > 0, ErrorCode.OPERATION_ERROR, "改期时间与已有课程冲突，请重新选择");

        TutorAppointment toUpdate = TutorAppointment.builder()
                .id(db.getId())
                .status(STATUS_RESCHEDULE_PENDING)
                .proposedStartTime(request.getProposedStartTime())
                .proposedBy(uid)
                .durationMinutes(duration)
                .remark(request.getRemark())
                .build();
        int updated = tutorAppointmentMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
        emailNotificationService.cancelLessonStartTasks(db.getId(), "lesson reschedule requested");
    }

    @Override
    public void confirmReschedule(Long id, Long uid) {
        TutorAppointment db = requireParticipant(id, uid);
        ThrowUtils.throwIf(db.getStatus() == null || db.getStatus() != STATUS_RESCHEDULE_PENDING, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(db.getProposedBy() == null, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(uid.equals(db.getProposedBy()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(db.getProposedStartTime() == null, ErrorCode.OPERATION_ERROR);
        int minutes = db.getDurationMinutes() == null ? 60 : db.getDurationMinutes();
        LocalDateTime proposedEnd = db.getProposedStartTime().plusMinutes(minutes);
        int conflicts = tutorAppointmentMapper.countAcceptedConflictsExcept(
                List.of(db.getParentId(), db.getTutorId()),
                db.getProposedStartTime(),
                proposedEnd,
                db.getId()
        );
        ThrowUtils.throwIf(conflicts > 0, ErrorCode.OPERATION_ERROR, "改期时间与已有课程冲突，请重新选择");

        int updated = tutorAppointmentMapper.confirmReschedule(db.getId());
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
        TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
        emailNotificationService.cancelLessonStartTasks(db.getId(), "lesson rescheduled");
        emailNotificationService.createLessonStartTasks(latest == null ? db : latest);
    }

    @Override
    public void cancel(Long id, String remark, Long uid) {
        TutorAppointment db = requireParticipant(id, uid);
        ThrowUtils.throwIf(db.getStatus() == null, ErrorCode.OPERATION_ERROR);
        ThrowUtils.throwIf(db.getStatus() == STATUS_CANCELED, ErrorCode.OPERATION_ERROR);

        TutorAppointment toUpdate = TutorAppointment.builder()
                .id(db.getId())
                .status(STATUS_CANCELED)
                .cancelBy(uid)
                .remark(remark)
                .build();
        int updated = tutorAppointmentMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
        emailNotificationService.cancelLessonStartTasks(db.getId(), "lesson canceled");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, Long uid) {
        TutorAppointment db = requireParticipant(id, uid);
        ThrowUtils.throwIf(db.getStatus() == null || db.getStatus() != STATUS_ACCEPTED, ErrorCode.OPERATION_ERROR, "当前状态不允许结课");
        ThrowUtils.throwIf(db.getStartTime() == null, ErrorCode.OPERATION_ERROR);

        int updated = tutorAppointmentMapper.completeIfAccepted(db.getId());
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        if (db.getParentJobPostingId() != null) {
            StudentJobPosting toUpdate = StudentJobPosting.builder()
                    .id(db.getParentJobPostingId())
                    .bizStatus(STATUS_COMPLETED)
                    .status(0)
                    .build();
            studentJobPostingMapper.updateById(toUpdate);
        }
        TutorAppointment latest = tutorAppointmentMapper.selectById(db.getId());
        TutorAppointment billable = latest == null ? db : latest;
        if (billable.getCourseId() != null && billable.getPayableAmountFen() != null && billable.getPayableAmountFen() > 0) {
            lessonPaymentOrderService.createAfterLessonCompleted(billable);
        }
        emailNotificationService.cancelLessonStartTasks(db.getId(), "lesson completed");
    }

    @Override
    public TutorAppointment detail(Long id, Long uid) {
        return requireParticipant(id, uid);
    }

    @Override
    public CursorPageResponse<TutorAppointment> mine(Long uid, Integer status, CursorPageRequest request) {
        ThrowUtils.throwIf(uid == null || request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();
        List<TutorAppointment> list = tutorAppointmentMapper.listByUser(uid, status, request.getCursor(), pageSize);
        Long nextCursor = null;
        if (list != null && !list.isEmpty()) {
            nextCursor = list.get(list.size() - 1).getId();
        }
        boolean isLast = list == null || list.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }

    private TutorAppointment requireParticipant(Long id, Long uid) {
        ThrowUtils.throwIf(id == null || uid == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment db = tutorAppointmentMapper.selectById(id);
        ThrowUtils.throwIf(db == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(db.getParentId()) && !uid.equals(db.getTutorId()), ErrorCode.NO_AUTH_ERROR);
        return db;
    }
}
