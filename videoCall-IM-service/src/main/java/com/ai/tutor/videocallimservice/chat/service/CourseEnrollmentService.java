package com.ai.tutor.videocallimservice.chat.service;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import com.ai.tutor.videocallimservice.chat.domain.entity.RefundRequest;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.CourseEnrollmentStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.RefundRequestStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.RefundRequestType;
import com.ai.tutor.videocallimservice.chat.domain.enums.TutorApplicationChatAccessStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.TutorApplicationStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ApplyTrialRefundReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SubmitTrialResultReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseDetailVO;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseItemVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RefundRequestMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import com.ai.tutor.videocallimservice.integration.feign.AppointmentInternalFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CourseEnrollmentService {

    @Resource
    private CourseEnrollmentMapper courseEnrollmentMapper;
    @Resource
    private RefundRequestMapper refundRequestMapper;
    @Resource
    private BrokerageOrderMapper brokerageOrderMapper;
    @Resource
    private CollaborationProposalMapper collaborationProposalMapper;
    @Resource
    private TutorApplicationMapper tutorApplicationMapper;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private ChatService chatService;
    @Resource
    private BizKpiMetrics bizKpiMetrics;
    @Autowired(required = false)
    private AppointmentInternalClient appointmentInternalClient;

    @Transactional(rollbackFor = Exception.class)
    public CourseEnrollment ensureForApplication(TutorApplication application) {
        if (application == null || application.getId() == null) {
            return null;
        }
        CourseEnrollment existing = courseEnrollmentMapper.selectByApplicationId(application.getId());
        if (existing != null) {
            if (existing.getRoomId() == null && application.getRoomId() != null) {
                courseEnrollmentMapper.updateRoomId(application.getId(), application.getRoomId());
            }
            return existing;
        }
        if (TutorApplicationStatus.REJECTED.name().equals(application.getStatus())) {
            return null;
        }

        Long teacherUid = resolveTeacherUid(application);
        Long studentUid = resolveStudentUid(application);
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.OPERATION_ERROR, "申请记录缺少教师/学生信息");

        LocalDateTime now = LocalDateTime.now();
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .applicationId(application.getId())
                .roomId(application.getRoomId())
                .proposalId(null)
                .teacherUid(teacherUid)
                .studentUid(studentUid)
                .teachingMode(normalizeTeachingMode(application.getTeachingMode()))
                .courseName(null)
                .classTime(null)
                .frequencyPerWeek(null)
                .lessonPrice(null)
                .status(deriveStatus(application))
                .trialStartAt(null)
                .trialEndAt(null)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            courseEnrollmentMapper.insert(enrollment);
        } catch (DuplicateKeyException e) {
            return courseEnrollmentMapper.selectByApplicationId(application.getId());
        }
        return enrollment;
    }

    public void onApplicationAccepted(TutorApplication application) {
        CourseEnrollment enrollment = ensureForApplication(application);
        if (enrollment == null || enrollment.getId() == null) {
            return;
        }
        courseEnrollmentMapper.updateStatus(enrollment.getId(), enrollment.getStatus(), CourseEnrollmentStatus.WAIT_PAY.name(), null, null, null);
    }

    public void onPaymentSuccess(TutorApplication application) {
        CourseEnrollment enrollment = ensureForApplication(application);
        if (enrollment == null || enrollment.getId() == null) {
            return;
        }
        int updated = courseEnrollmentMapper.updateStatus(enrollment.getId(), enrollment.getStatus(), CourseEnrollmentStatus.COMMUNICATING.name(), null, null, null);
        if (updated <= 0 && bizKpiMetrics != null) {
            /*
             * 中文注释：支付成功但课程状态未能顺利切到 COMMUNICATING 时记一次失败指标，
             * 用来尽快发现“付费成功但业务状态没跟上”的异常。
             */
            bizKpiMetrics.incChatUnlockFailed("course_status_not_updated");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onCollaborationAccepted(Long roomId, Long proposalId) {
        if (roomId == null) {
            return;
        }
        CourseEnrollment enrollment = courseEnrollmentMapper.selectLatestByRoomId(roomId);
        if (enrollment == null || enrollment.getId() == null || enrollment.getStatus() == null) {
            return;
        }
        if (!CourseEnrollmentStatus.COMMUNICATING.name().equals(enrollment.getStatus())
                && !CourseEnrollmentStatus.WAIT_PAY.name().equals(enrollment.getStatus())) {
            return;
        }
        TutorApplication application = tutorApplicationMapper.selectLatestByRoomId(roomId);
        CollaborationProposal proposal = proposalId == null ? null : collaborationProposalMapper.selectById(proposalId);
        LocalDateTime start = proposal == null || proposal.getTrialStartAt() == null ? LocalDateTime.now() : proposal.getTrialStartAt();
        LocalDateTime end = proposal == null || proposal.getTrialEndAt() == null ? start.plus(2, ChronoUnit.HOURS) : proposal.getTrialEndAt();
        // 中文注释：线上合作一旦被接受，就把聊天阶段沉淀为“长期课程”，后续短期课节必须挂在这门课下面。
        int updated = courseEnrollmentMapper.startOnlineCourse(
                enrollment.getId(),
                enrollment.getStatus(),
                proposalId,
                normalizeTeachingMode(application == null ? null : application.getTeachingMode()),
                buildCourseName(proposal, application, enrollment),
                proposal == null ? null : trimTo255(proposal.getClassTime()),
                proposal == null ? null : proposal.getFrequencyPerWeek(),
                proposal == null ? null : trimTo64(proposal.getPricePerHour()),
                start,
                end
        );
        if (updated > 0) {
            if (bizKpiMetrics != null) {
                /*
                 * 中文注释：试课安排成功只在课程首次成功进入试课阶段时累计，
                 * 避免补偿执行或重复点击把同一门课重复算成多次安排。
                 */
                bizKpiMetrics.incTrialScheduled();
            }
            CourseEnrollment latest = courseEnrollmentMapper.selectById(enrollment.getId());
            ensureAcceptedTrialSchedule(latest == null ? enrollment : latest, proposal);
        }
    }

    public List<CourseItemVO> listMyCourses(Long uid, String role, int page, int size) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        int pageSize = Math.max(1, Math.min(size <= 0 ? 20 : size, 50));
        int pageNo = Math.max(1, page);
        long offset = (long) (pageNo - 1) * pageSize;

        List<CourseEnrollment> rows;
        if ("STUDENT".equalsIgnoreCase(role)) {
            rows = courseEnrollmentMapper.listByStudent(uid, offset, pageSize);
        } else {
            rows = courseEnrollmentMapper.listByTeacher(uid, offset, pageSize);
        }
        List<CourseItemVO> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (CourseEnrollment e : rows) {
            ensureAcceptedTrialScheduleIfNeeded(e, false);
            out.add(CourseItemVO.builder()
                    .courseId(e.getId())
                    .applicationId(e.getApplicationId())
                    .roomId(e.getRoomId())
                    .teacherUid(e.getTeacherUid())
                    .studentUid(e.getStudentUid())
                    .teachingMode(e.getTeachingMode())
                    .courseName(e.getCourseName())
                    .classTime(e.getClassTime())
                    .frequencyPerWeek(e.getFrequencyPerWeek())
                    .lessonPrice(e.getLessonPrice())
                    .status(e.getStatus())
                    .trialStartAt(e.getTrialStartAt())
                    .trialEndAt(e.getTrialEndAt())
                    .build());
        }
        return out;
    }

    public CourseDetailVO getCourseDetail(Long courseId, Long uid) {
        ThrowUtils.throwIf(courseId == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectById(courseId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        assertParticipant(course, uid);
        ensureAcceptedTrialScheduleIfNeeded(course);
        return toCourseDetail(course);
    }

    public CourseDetailVO getCourseByRoom(Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectLatestByRoomId(roomId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "当前会话暂无长期课程");
        assertParticipant(course, uid);
        ensureAcceptedTrialScheduleIfNeeded(course);
        return toCourseDetail(course);
    }

    @Scheduled(fixedDelayString = "${course.trial.scheduler-delay-ms:60000}")
    public void processEndedTrials() {
        List<CourseEnrollment> list = courseEnrollmentMapper.listTrialingEnded(LocalDateTime.now(), 50);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (CourseEnrollment course : list) {
            if (course == null || course.getId() == null) {
                continue;
            }
            int updated = courseEnrollmentMapper.updateStatus(
                    course.getId(),
                    CourseEnrollmentStatus.TRIALING.name(),
                    CourseEnrollmentStatus.TRIAL_WAIT_STUDENT_DECISION.name(),
                    null,
                    null,
                    null
            );
            if (updated > 0 && bizKpiMetrics != null) {
                /*
                 * 中文注释：试课结束指标只在系统首次把课程推进到“待学生决策”时累计，
                 * 用于衡量真正进入后续转化池的试课数量。
                 */
                bizKpiMetrics.incTrialFinished();
            }
        }
    }

    @Scheduled(fixedDelayString = "${course.weekly-schedule.scheduler-delay-ms:60000}")
    public void processWeeklyScheduleDeadlineTasks() {
        processWeeklyScheduleTimeouts();
        processWeeklyScheduleReminders();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitTrialResult(Long courseId, SubmitTrialResultReq req, Long uid) {
        ThrowUtils.throwIf(courseId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectById(courseId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        ThrowUtils.throwIf(!uid.equals(course.getStudentUid()), ErrorCode.NO_AUTH_ERROR, "试课后是否继续只能由学生确认");
        String result = req.getResult() == null ? "" : req.getResult().trim().toUpperCase();
        if ("PASS".equals(result)) {
            confirmTrialPassedByStudent(course);
            return;
        }
        ThrowUtils.throwIf(!"FAIL".equals(result), ErrorCode.PARAMS_ERROR, "试课结果不合法");

        ApplyTrialRefundReq refundReq = new ApplyTrialRefundReq();
        refundReq.setReason(req.getReason());
        refundReq.setEvidenceImageUrls(req.getEvidenceImageUrls());
        refundReq.setEvidenceVideoUrl(req.getEvidenceVideoUrl());
        refundReq.setEvidenceVideoDurationSeconds(req.getEvidenceVideoDurationSeconds());
        failOnlineTrial(course, refundReq);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmWeeklyScheduleSubmitted(Long courseId, Long uid, String classTime, Integer frequencyPerWeek, Long lessonPriceFen) {
        ThrowUtils.throwIf(courseId == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectById(courseId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        ThrowUtils.throwIf(!uid.equals(course.getStudentUid()), ErrorCode.NO_AUTH_ERROR, "正式每周课表只能由学生提交");
        if (CourseEnrollmentStatus.TEACHING.name().equals(course.getStatus())) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "正式课表已提交，不能重复提交");
        }
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可提交正式课表");
        int updated = courseEnrollmentMapper.markWeeklyScheduleSubmitted(
                course.getId(),
                CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name(),
                trimTo255(classTime),
                frequencyPerWeek,
                formatLessonPriceFen(lessonPriceFen)
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "正式课表提交状态更新失败，请稍后重试");
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：正式课表提交指标只在学生首次成功提交并完成状态更新后累计，
             * 避免重复提交前后的多次请求把长期转化虚高。
             */
            bizKpiMetrics.incWeeklyScheduleSubmitted();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markTrialCanceled(Long courseId, Long uid, String reason) {
        ThrowUtils.throwIf(courseId == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectById(courseId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        assertParticipant(course, uid);
        if (CourseEnrollmentStatus.COMMUNICATING.name().equals(course.getStatus())) {
            reopenCourseCommunication(course);
            return;
        }
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIALING.name().equals(course.getStatus())
                && !CourseEnrollmentStatus.TRIAL_WAIT_STUDENT_DECISION.name().equals(course.getStatus())
                && !CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可取消试课");
        int updated = courseEnrollmentMapper.updateStatus(
                course.getId(),
                course.getStatus(),
                CourseEnrollmentStatus.COMMUNICATING.name(),
                null,
                null,
                null
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "试课取消状态更新失败，请稍后重试");
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：试课取消指标只在试课成功回退到沟通阶段后累计，
             * 这样能真实反映履约前取消发生量。
             */
            bizKpiMetrics.incTrialCancel(uid.equals(course.getTeacherUid()) ? "teacher" : "student");
        }
        reopenCourseCommunication(course);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long applyTrialRefund(Long courseId, ApplyTrialRefundReq req, Long uid) {
        ThrowUtils.throwIf(courseId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectById(courseId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        ThrowUtils.throwIf(!uid.equals(course.getTeacherUid()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIALING.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可发起试课退费");
        ThrowUtils.throwIf(course.getTrialEndAt() == null || LocalDateTime.now().isAfter(course.getTrialEndAt()), ErrorCode.OPERATION_ERROR, "试课期已结束，无法发起试课退费");
        ThrowUtils.throwIf("ONLINE".equalsIgnoreCase(course.getTeachingMode()), ErrorCode.OPERATION_ERROR, "线上试课不支持退还信息费，请直接发起退课");
        ThrowUtils.throwIf(req.getEvidenceVideoUrl() == null || req.getEvidenceVideoUrl().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "请上传微信聊天录屏");
        ThrowUtils.throwIf(req.getEvidenceVideoDurationSeconds() == null || req.getEvidenceVideoDurationSeconds() <= 0 || req.getEvidenceVideoDurationSeconds() > 60,
                ErrorCode.PARAMS_ERROR,
                "录屏时长需控制在 60 秒内");

        BrokerageOrder order = brokerageOrderMapper.selectByApplicationId(course.getApplicationId());
        ThrowUtils.throwIf(order == null || order.getId() == null, ErrorCode.NOT_FOUND_ERROR, "未找到信息费订单");
        ThrowUtils.throwIf(!BrokerageOrderStatus.PAID.name().equals(order.getStatus()), ErrorCode.OPERATION_ERROR, "信息费订单未支付或已不可退");

        RefundRequest pending = refundRequestMapper.selectPendingByBrokerageOrderId(order.getId());
        if (pending != null && pending.getId() != null) {
            return pending.getId();
        }

        int locked = brokerageOrderMapper.lockForRefund(order.getId(), BrokerageOrderStatus.TRIAL_REFUND_REVIEW.name());
        if (locked <= 0) {
            RefundRequest latest = refundRequestMapper.selectPendingByBrokerageOrderId(order.getId());
            if (latest != null && latest.getId() != null) {
                return latest.getId();
            }
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "订单状态不可申请试课退费");
        }

        long refundAmountFen = computePercentAmount(order.getAmountFen(), 80);
        LocalDateTime now = LocalDateTime.now();
        RefundRequest request = RefundRequest.builder()
                .brokerageOrderId(order.getId())
                .courseId(course.getId())
                .roomId(course.getRoomId())
                .applicantUid(uid)
                .applicantRole("TEACHER")
                .type(RefundRequestType.TRIAL_INFO_FEE.name())
                .status(RefundRequestStatus.PENDING.name())
                .reason(trimTo1024(req.getReason()))
                .evidenceImagesJson(JSONUtil.toJsonStr(req.getEvidenceImageUrls()))
                .evidenceVideoUrl(trimTo1024(req.getEvidenceVideoUrl()))
                .evidenceVideoDurationSeconds(req.getEvidenceVideoDurationSeconds())
                .evidenceVideoDeleteStatus("PENDING_DELETE")
                .refundPercent(80)
                .refundAmountFen(refundAmountFen)
                .adminUid(null)
                .adminNote(null)
                .decidedAt(null)
                .createTime(now)
                .updateTime(now)
                .build();
        refundRequestMapper.insert(request);
        ThrowUtils.throwIf(request.getId() == null, ErrorCode.OPERATION_ERROR);
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：退款申请指标只在退款申请记录真正写入后计数，便于和最终退款成功拆开看。
             */
            bizKpiMetrics.incRefundRequest("trial_info_fee");
        }

        courseEnrollmentMapper.updateStatus(course.getId(), course.getStatus(), CourseEnrollmentStatus.TRIAL_REFUND_REVIEW.name(), null, null, null);
        closeCourseCommunication(course);
        return request.getId();
    }

    private void confirmTrialPassedByStudent(CourseEnrollment course) {
        ThrowUtils.throwIf(course == null || course.getId() == null, ErrorCode.PARAMS_ERROR);
        if (CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name().equals(course.getStatus())
                || CourseEnrollmentStatus.TEACHING.name().equals(course.getStatus())) {
            return;
        }
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIAL_WAIT_STUDENT_DECISION.name().equals(course.getStatus())
                && !CourseEnrollmentStatus.TRIALING.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可确认试课结果");
        int updated = courseEnrollmentMapper.markTrialPassedWaitingWeeklySchedule(
                course.getId(),
                course.getStatus(),
                resolveWeeklyScheduleDeadline(course)
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "试课结果提交失败，请稍后重试");
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：试课通过指标只在学生确认通过且状态成功切换后累计，避免重复点击导致双计数。
             */
            bizKpiMetrics.incTrialDecision("passed");
        }
        CourseEnrollment latest = courseEnrollmentMapper.selectById(course.getId());
        sendWeeklyScheduleReminderMessage(latest == null ? course : latest, "试课已通过，请在试课结束后 24 小时内确认正式每周课表。");
    }

    private void failOnlineTrial(CourseEnrollment course, ApplyTrialRefundReq req) {
        ThrowUtils.throwIf(course == null || course.getId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIALING.name().equals(course.getStatus())
                && !CourseEnrollmentStatus.TRIAL_WAIT_STUDENT_DECISION.name().equals(course.getStatus())
                && !CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可发起退课");
        ThrowUtils.throwIf(trimTo1024(req == null ? null : req.getReason()) == null, ErrorCode.PARAMS_ERROR, "请填写试课不通过说明");
        int updated = courseEnrollmentMapper.updateStatus(
                course.getId(),
                course.getStatus(),
                CourseEnrollmentStatus.TRIAL_FAILED.name(),
                null,
                null,
                null
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "试课结果提交失败，请稍后重试");
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：试课失败指标只在课程首次成功进入 TRIAL_FAILED 后累计，
             * 便于稳定观察试课转长期课阶段的损耗。
             */
            bizKpiMetrics.incTrialDecision("failed");
        }
        closeCourseCommunication(course);
    }

    private void ensureAcceptedTrialScheduleIfNeeded(CourseEnrollment course) {
        ensureAcceptedTrialScheduleIfNeeded(course, true);
    }

    private void ensureAcceptedTrialScheduleIfNeeded(CourseEnrollment course, boolean failFast) {
        if (!shouldEnsureAcceptedTrialSchedule(course)) {
            return;
        }
        CollaborationProposal proposal = course.getProposalId() == null ? null : collaborationProposalMapper.selectById(course.getProposalId());
        try {
            ensureAcceptedTrialSchedule(course, proposal);
        } catch (Exception ex) {
            if (failFast) {
                throw ex;
            }
            log.warn("best effort ensure accepted trial schedule failed, courseId={}, proposalId={}",
                    course.getId(),
                    course.getProposalId(),
                    ex);
        }
    }

    private boolean shouldEnsureAcceptedTrialSchedule(CourseEnrollment course) {
        if (course == null || course.getId() == null || course.getStatus() == null) {
            return false;
        }
        String status = course.getStatus().trim().toUpperCase();
        return CourseEnrollmentStatus.TRIALING.name().equals(status)
                || CourseEnrollmentStatus.TRIAL_WAIT_STUDENT_DECISION.name().equals(status)
                || CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name().equals(status)
                || CourseEnrollmentStatus.TEACHING.name().equals(status);
    }

    private void ensureAcceptedTrialSchedule(CourseEnrollment course, CollaborationProposal proposal) {
        if (appointmentInternalClient == null
                || course == null
                || course.getId() == null
                || course.getTrialStartAt() == null
                || course.getTrialEndAt() == null) {
            return;
        }
        AppointmentInternalFeignClient.InternalTrialEventRequest request = new AppointmentInternalFeignClient.InternalTrialEventRequest();
        request.setCourseId(course.getId());
        request.setRoomId(course.getRoomId());
        request.setTeacherUid(course.getTeacherUid());
        request.setStudentUid(course.getStudentUid());
        request.setCreatedBy(proposal == null ? course.getTeacherUid() : proposal.getFromUid());
        request.setTitle("试课｜" + (course.getCourseName() == null ? "线上长期课程" : course.getCourseName()));
        request.setLessonPrice(course.getLessonPrice());
        request.setStartAt(toEpochMillis(course.getTrialStartAt()));
        request.setEndAt(toEpochMillis(course.getTrialEndAt()));
        request.setRemark(proposal == null ? null : proposal.getRemark());
        request.setClientRequestId(proposal == null || proposal.getId() == null ? null : "COLLAB_TRIAL:" + proposal.getId());
        try {
            Long appointmentId = appointmentInternalClient.createAcceptedTrialEvent(request);
            log.info("ensure accepted trial schedule success, courseId={}, proposalId={}, appointmentId={}",
                    course.getId(),
                    proposal == null ? null : proposal.getId(),
                    appointmentId);
        } catch (Exception ex) {
            log.error("ensure accepted trial schedule failed, courseId={}, proposalId={}, roomId={}",
                    course.getId(),
                    proposal == null ? null : proposal.getId(),
                    course.getRoomId(),
                    ex);
            throw ex;
        }
    }

    private void closeCourseCommunication(CourseEnrollment course) {
        if (course == null) {
            return;
        }
        if (course.getApplicationId() != null) {
            tutorApplicationMapper.updateChatAccessStatus(course.getApplicationId(), TutorApplicationChatAccessStatus.NONE.name());
        }
        if (course.getRoomId() != null) {
            roomMapper.closeRoom(course.getRoomId());
        }
    }

    private void reopenCourseCommunication(CourseEnrollment course) {
        if (course == null) {
            return;
        }
        if (course.getApplicationId() != null) {
            tutorApplicationMapper.updateChatAccessStatus(course.getApplicationId(), TutorApplicationChatAccessStatus.CHAT_ENABLED.name());
        }
        if (course.getRoomId() != null) {
            roomMapper.reopenRoom(course.getRoomId());
        }
    }

    private void processWeeklyScheduleTimeouts() {
        List<CourseEnrollment> list = courseEnrollmentMapper.listWeeklyScheduleDeadlineReached(LocalDateTime.now(), 50);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (CourseEnrollment course : list) {
            if (course == null || course.getId() == null) {
                continue;
            }
            int updated = courseEnrollmentMapper.updateStatus(
                    course.getId(),
                    CourseEnrollmentStatus.TRIAL_WAIT_WEEKLY_SCHEDULE.name(),
                    CourseEnrollmentStatus.TRIAL_FAILED.name(),
                    null,
                    null,
                    null
            );
            if (updated > 0) {
                if (bizKpiMetrics != null) {
                    /*
                     * 中文注释：正式课表超时指标只在系统首次判定超时失败时累计，
                     * 用于定位“试课通过后迟迟不提交正式课表”的流失。
                     */
                    bizKpiMetrics.incWeeklyScheduleTimeout();
                }
                CourseEnrollment latest = courseEnrollmentMapper.selectById(course.getId());
                sendWeeklyScheduleReminderMessage(latest == null ? course : latest, "正式课表确认已超时，系统已判定试课失败。");
                closeCourseCommunication(course);
            }
        }
    }

    private void processWeeklyScheduleReminders() {
        List<CourseEnrollment> list;
        try {
            list = courseEnrollmentMapper.listWeeklyScheduleReminderDue(LocalDateTime.now(), 50);
        } catch (Exception ex) {
            log.warn("skip weekly schedule reminders because reminder columns may be unavailable", ex);
            return;
        }
        if (list == null || list.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (CourseEnrollment course : list) {
            if (course == null || course.getId() == null || course.getWeeklyScheduleDeadlineAt() == null) {
                continue;
            }
            long minutesLeft = ChronoUnit.MINUTES.between(now, course.getWeeklyScheduleDeadlineAt());
            String column;
            String label;
            if (minutesLeft <= 60) {
                column = "weekly_reminder_1h_sent_at";
                label = "1 小时";
            } else if (minutesLeft <= 6 * 60) {
                column = "weekly_reminder_6h_sent_at";
                label = "6 小时";
            } else {
                column = "weekly_reminder_12h_sent_at";
                label = "12 小时";
            }
            try {
                courseEnrollmentMapper.markWeeklyReminderSent(course.getId(), column, now);
            } catch (Exception ex) {
                log.warn("skip markWeeklyReminderSent for courseId={} column={}", course.getId(), column, ex);
            }
            sendWeeklyScheduleReminderMessage(course, "请尽快确认正式每周课表，距离截止还有 " + label + "。");
        }
    }

    private void sendWeeklyScheduleReminderMessage(CourseEnrollment course, String content) {
        if (course == null || course.getRoomId() == null || content == null || content.isBlank()) {
            return;
        }
        SystemMsgReq payload = new SystemMsgReq();
        payload.setBizType("COURSE_STATUS_REMINDER");
        payload.setEventId(course.getId());
        payload.setStatus(course.getStatus());
        payload.setContent(content);
        payload.setContextType("COURSE");
        payload.setContextId(course.getId());
        payload.setTitle(course.getCourseName());
        if (course.getWeeklyScheduleDeadlineAt() != null) {
            payload.setEndAt(toEpochMillis(course.getWeeklyScheduleDeadlineAt()));
        }
        sendCourseSystemMessage(course, payload);
    }

    private void sendCourseSystemMessage(CourseEnrollment course, Object payload) {
        if (chatService == null || course == null || course.getRoomId() == null || payload == null) {
            return;
        }
        try {
            chatService.sendMsg(ChatMessageReq.builder()
                    .roomId(course.getRoomId())
                    .msgType(8)
                    .body(payload)
                    .build(), course.getStudentUid());
        } catch (Exception ignored) {
            // 系统提醒不影响主链路，失败后由下一轮任务继续兜底。
        }
    }

    private static LocalDateTime resolveWeeklyScheduleDeadline(CourseEnrollment course) {
        if (course == null || course.getTrialEndAt() == null) {
            return LocalDateTime.now().plusHours(24);
        }
        return course.getTrialEndAt().plusHours(24);
    }

    private static long computePercentAmount(Long amountFen, int percent) {
        if (amountFen == null || amountFen <= 0) {
            return 0L;
        }
        return amountFen * percent / 100;
    }

    private static String deriveStatus(TutorApplication application) {
        String st = application.getStatus();
        if (TutorApplicationStatus.PENDING.name().equals(st)) {
            return CourseEnrollmentStatus.APPLYING.name();
        }
        if (TutorApplicationStatus.ACCEPTED.name().equals(st)) {
            String access = application.getChatAccessStatus();
            if (TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name().equals(access)) {
                return CourseEnrollmentStatus.WAIT_PAY.name();
            }
            if (TutorApplicationChatAccessStatus.CHAT_ENABLED.name().equals(access)) {
                return CourseEnrollmentStatus.COMMUNICATING.name();
            }
            return CourseEnrollmentStatus.WAIT_PAY.name();
        }
        return CourseEnrollmentStatus.APPLYING.name();
    }

    private static Long resolveTeacherUid(TutorApplication application) {
        String senderRole = application.getSenderRole() == null ? "" : application.getSenderRole().trim().toUpperCase();
        if ("TEACHER".equals(senderRole)) {
            return application.getSenderUid();
        }
        return application.getReceiverUid();
    }

    private static Long resolveStudentUid(TutorApplication application) {
        String senderRole = application.getSenderRole() == null ? "" : application.getSenderRole().trim().toUpperCase();
        if ("STUDENT".equals(senderRole)) {
            return application.getSenderUid();
        }
        return application.getReceiverUid();
    }

    private static void assertParticipant(CourseEnrollment course, Long uid) {
        ThrowUtils.throwIf(course == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!uid.equals(course.getTeacherUid()) && !uid.equals(course.getStudentUid()), ErrorCode.NO_AUTH_ERROR);
    }

    private static CourseDetailVO toCourseDetail(CourseEnrollment course) {
        return CourseDetailVO.builder()
                .courseId(course.getId())
                .applicationId(course.getApplicationId())
                .roomId(course.getRoomId())
                .teacherUid(course.getTeacherUid())
                .studentUid(course.getStudentUid())
                .teachingMode(course.getTeachingMode())
                .courseName(course.getCourseName())
                .classTime(course.getClassTime())
                .frequencyPerWeek(course.getFrequencyPerWeek())
                .lessonPrice(course.getLessonPrice())
                .status(course.getStatus())
                .trialStartAt(course.getTrialStartAt())
                .trialEndAt(course.getTrialEndAt())
                .weeklyScheduleDeadlineAt(course.getWeeklyScheduleDeadlineAt())
                .weeklyScheduleSubmittedAt(course.getWeeklyScheduleSubmittedAt())
                .build();
    }

    private static String formatLessonPriceFen(Long lessonPriceFen) {
        if (lessonPriceFen == null || lessonPriceFen <= 0) {
            return null;
        }
        if (lessonPriceFen % 100 == 0) {
            return (lessonPriceFen / 100) + " 元/节";
        }
        return String.format(java.util.Locale.ROOT, "%.2f 元/节", lessonPriceFen / 100.0);
    }

    private static String normalizeTeachingMode(String teachingMode) {
        String mode = teachingMode == null ? "" : teachingMode.trim().toUpperCase();
        if ("ONLINE".equals(mode) || "OFFLINE".equals(mode)) {
            return mode;
        }
        return null;
    }

    private static String buildCourseName(CollaborationProposal proposal, TutorApplication application, CourseEnrollment enrollment) {
        String lessonPrice = proposal == null ? null : trimTo64(proposal.getPricePerHour());
        String classTime = proposal == null ? null : trimTo255(proposal.getClassTime());
        if (lessonPrice != null && classTime != null) {
            return trimTo255("线上一对一｜" + lessonPrice + "｜" + classTime);
        }
        if (classTime != null) {
            return trimTo255("线上一对一｜" + classTime);
        }
        if (application != null && "ONLINE".equalsIgnoreCase(application.getTeachingMode())) {
            return "线上长期课程";
        }
        return enrollment.getCourseName() == null ? "长期课程" : enrollment.getCourseName();
    }

    private static String trimTo1024(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= 1024) return v;
        return v.substring(0, 1024);
    }

    private static String trimTo255(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= 255) return v;
        return v.substring(0, 255);
    }

    private static String trimTo64(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= 64) return v;
        return v.substring(0, 64);
    }

    private static Long toEpochMillis(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }
}
