package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.LessonPaymentOrderMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.TeacherSettlementMapper;
import com.ai.tutor.appointment.model.entity.LessonPaymentOrder;
import com.ai.tutor.appointment.model.entity.TeacherSettlement;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.service.LessonPaymentOrderService;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.LessonPaymentAccessCheckInfo;
import com.ai.tutor.common.integration.LessonPaymentPayInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class LessonPaymentOrderServiceImpl implements LessonPaymentOrderService {

    @Resource
    private LessonPaymentOrderMapper lessonPaymentOrderMapper;

    @Resource
    private TeacherSettlementMapper teacherSettlementMapper;
    @Resource
    private TutorAppointmentMapper tutorAppointmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonPaymentOrder createAfterLessonCompleted(TutorAppointment appointment) {
        ThrowUtils.throwIf(appointment == null || appointment.getId() == null, ErrorCode.PARAMS_ERROR);
        LessonPaymentOrder existing = lessonPaymentOrderMapper.selectByLessonId(appointment.getId());
        if (existing != null) {
            return existing;
        }
        return null;
    }

    @Override
    public LessonPaymentPayInfo getPayableOrder(Long orderId, Long uid) {
        ThrowUtils.throwIf(orderId == null || uid == null, ErrorCode.PARAMS_ERROR);
        LessonPaymentOrder order = lessonPaymentOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR, "课节支付单不存在");
        ThrowUtils.throwIf(!uid.equals(order.getStudentUid()), ErrorCode.NO_AUTH_ERROR, "仅学生端可支付课时费");
        ThrowUtils.throwIf("PAID".equalsIgnoreCase(order.getStatus()), ErrorCode.OPERATION_ERROR, "该课节已支付");
        ThrowUtils.throwIf("CANCELED".equalsIgnoreCase(order.getStatus()), ErrorCode.OPERATION_ERROR, "该课节账单已取消");
        lessonPaymentOrderMapper.markPaying(order.getId());
        LessonPaymentOrder latest = lessonPaymentOrderMapper.selectById(order.getId());
        LessonPaymentPayInfo info = new LessonPaymentPayInfo();
        info.setOrderId(latest.getId());
        info.setLessonId(latest.getLessonId());
        info.setCourseId(latest.getCourseId());
        info.setPayerUid(latest.getStudentUid());
        info.setTeacherUid(latest.getTeacherUid());
        info.setAmountFen(latest.getTotalAmountFen());
        info.setStatus(latest.getStatus());
        info.setSubject(buildSubject(latest));
        info.setBody(buildBody(latest));
        return info;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        ThrowUtils.throwIf(event == null || event.getContextId() == null, ErrorCode.PARAMS_ERROR);
        LessonPaymentOrder order = lessonPaymentOrderMapper.selectById(event.getContextId());
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR, "课节支付单不存在");
        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            ensureTeacherSettlement(order);
            return;
        }
        lessonPaymentOrderMapper.markPaid(order.getId(), event.getOrderNo(), event.getSuccessTime() == null ? LocalDateTime.now() : event.getSuccessTime());
        LessonPaymentOrder latest = lessonPaymentOrderMapper.selectById(order.getId());
        ensureTeacherSettlement(latest);
    }

    @Override
    public LessonPaymentOrder getByLessonId(Long lessonId) {
        return lessonPaymentOrderMapper.selectByLessonId(lessonId);
    }

    @Override
    public LessonPaymentOrder findUnpaidByCourseId(Long courseId) {
        return null;
    }

    @Override
    public LessonPaymentAccessCheckInfo checkJoinAccessByLessonId(Long lessonId) {
        ThrowUtils.throwIf(lessonId == null, ErrorCode.PARAMS_ERROR);
        TutorAppointment appointment = tutorAppointmentMapper.selectById(lessonId);
        ThrowUtils.throwIf(appointment == null, ErrorCode.NOT_FOUND_ERROR, "课节不存在");
        LessonPaymentAccessCheckInfo info = new LessonPaymentAccessCheckInfo();
        info.setLessonId(lessonId);
        info.setCourseId(appointment.getCourseId());
        info.setBlocked(Boolean.FALSE);
        return info;
    }

    private void ensureTeacherSettlement(LessonPaymentOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        TeacherSettlement settlement = teacherSettlementMapper.selectByLessonPaymentOrderId(order.getId());
        if (settlement != null) {
            return;
        }
        // 中文注释：当前仍是学生支付到平台账户，后续再对接微信商家转账/企业付款，把应收自动划转到教师微信。
        // TODO: 对接微信转账能力，按结算记录将教师收入打款到教师微信。
        teacherSettlementMapper.insertIgnore(TeacherSettlement.builder()
                .lessonPaymentOrderId(order.getId())
                .teacherUid(order.getTeacherUid())
                .settlementAmountFen(order.getTeacherIncomeAmountFen())
                .platformFeeAmountFen(order.getPlatformFeeAmountFen())
                .status("SETTLEABLE")
                .build());
    }

    private static String buildSubject(LessonPaymentOrder order) {
        return "课后支付";
    }

    private static String buildBody(LessonPaymentOrder order) {
        String lessonType = normalizeLessonType(order == null ? null : order.getLessonType());
        return "ONLINE_TRIAL".equals(lessonType) || "TRIAL".equals(lessonType) ? "试课课时费" : "课时费";
    }

    private static String normalizeLessonType(String lessonType) {
        String normalized = StringUtils.hasText(lessonType) ? lessonType.trim().toUpperCase() : "NORMAL";
        return "TRIAL".equals(normalized) ? "TRIAL" : "NORMAL";
    }

}
