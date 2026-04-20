package com.ai.tutor.videocallimservice.chat.service;

import cn.hutool.json.JSONUtil;
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
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SubmitTrialResultReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseDetailVO;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseItemVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RefundRequestMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
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
        courseEnrollmentMapper.updateStatus(enrollment.getId(), enrollment.getStatus(), CourseEnrollmentStatus.COMMUNICATING.name(), null, null, null);
    }

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
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plus(7, ChronoUnit.DAYS);
        // 中文注释：线上合作一旦被接受，就把聊天阶段沉淀为“长期课程”，后续短期课节必须挂在这门课下面。
        courseEnrollmentMapper.startOnlineCourse(
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
        return toCourseDetail(course);
    }

    public CourseDetailVO getCourseByRoom(Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectLatestByRoomId(roomId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "当前会话暂无长期课程");
        assertParticipant(course, uid);
        return toCourseDetail(course);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitTrialResult(Long courseId, SubmitTrialResultReq req, Long uid) {
        ThrowUtils.throwIf(courseId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);
        CourseEnrollment course = courseEnrollmentMapper.selectById(courseId);
        ThrowUtils.throwIf(course == null, ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        ThrowUtils.throwIf(!uid.equals(course.getTeacherUid()), ErrorCode.NO_AUTH_ERROR);
        String result = req.getResult() == null ? "" : req.getResult().trim().toUpperCase();
        if ("PASS".equals(result)) {
            confirmTrialPassed(course);
            return;
        }
        ThrowUtils.throwIf(!"FAIL".equals(result), ErrorCode.PARAMS_ERROR, "试课结果不合法");

        ApplyTrialRefundReq refundReq = new ApplyTrialRefundReq();
        refundReq.setReason(req.getReason());
        refundReq.setEvidenceImageUrls(req.getEvidenceImageUrls());
        refundReq.setEvidenceVideoUrl(req.getEvidenceVideoUrl());
        refundReq.setEvidenceVideoDurationSeconds(req.getEvidenceVideoDurationSeconds());
        if ("ONLINE".equalsIgnoreCase(course.getTeachingMode())) {
            failOnlineTrial(course, refundReq);
            return;
        }
        applyTrialRefund(courseId, refundReq, uid);
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

        courseEnrollmentMapper.updateStatus(course.getId(), course.getStatus(), CourseEnrollmentStatus.TRIAL_REFUND_REVIEW.name(), null, null, null);
        closeCourseCommunication(course);
        return request.getId();
    }

    private void confirmTrialPassed(CourseEnrollment course) {
        ThrowUtils.throwIf(course == null || course.getId() == null, ErrorCode.PARAMS_ERROR);
        if (CourseEnrollmentStatus.TEACHING.name().equals(course.getStatus())) {
            return;
        }
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIALING.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可确认试课结果");
        int updated = courseEnrollmentMapper.updateStatus(
                course.getId(),
                course.getStatus(),
                CourseEnrollmentStatus.TEACHING.name(),
                null,
                null,
                null
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "试课结果提交失败，请稍后重试");
    }

    private void failOnlineTrial(CourseEnrollment course, ApplyTrialRefundReq req) {
        ThrowUtils.throwIf(course == null || course.getId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!CourseEnrollmentStatus.TRIALING.name().equals(course.getStatus()), ErrorCode.OPERATION_ERROR, "当前课程状态不可发起退课");
        ThrowUtils.throwIf(trimTo1024(req == null ? null : req.getReason()) == null, ErrorCode.PARAMS_ERROR, "请填写试课不通过说明");
        int updated = courseEnrollmentMapper.updateStatus(
                course.getId(),
                course.getStatus(),
                CourseEnrollmentStatus.FINISHED.name(),
                null,
                null,
                null
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "试课结果提交失败，请稍后重试");
        closeCourseCommunication(course);
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
                .build();
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
}
