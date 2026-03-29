package com.ai.tutor.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 业务 KPI 指标打点封装。
 *
 * <p>设计目标：
 * <ul>
 *   <li>低侵入：业务代码只在关键“状态变更成功”点调用一次方法</li>
 *   <li>不影响主流程：未启用 Metrics 或导出异常时，全部降级为 no-op</li>
 *   <li>低基数：禁止 userId/orderNo/phone 等高基数标签，仅使用固定枚举维度</li>
 * </ul>
 *
 * <p>导出方式：
 * 各服务引入 actuator + prometheus registry 后，通过 {@code /actuator/prometheus} 被 Prometheus scrape。
 */
@Component
public class BizKpiMetrics {

    public static final String METRIC_USER_REGISTER_TOTAL = "ai_tutor_biz_user_register_total";
    public static final String METRIC_COMM_APPLY_TOTAL = "ai_tutor_biz_comm_apply_total";
    public static final String METRIC_COMM_APPLY_DECISION_TOTAL = "ai_tutor_biz_comm_apply_decision_total";
    public static final String METRIC_SMS_CODE_SEND_TOTAL = "ai_tutor_biz_sms_code_send_total";
    public static final String METRIC_PAYMENT_INFO_FEE_AMOUNT_CENTS_TOTAL = "ai_tutor_biz_payment_info_fee_amount_cents_total";
    public static final String METRIC_COLLABORATION_SUCCESS_TOTAL = "ai_tutor_biz_collaboration_success_total";
    public static final String METRIC_REFUND_TOTAL = "ai_tutor_biz_refund_total";
    public static final String METRIC_REFUND_AMOUNT_CENTS_TOTAL = "ai_tutor_biz_refund_amount_cents_total";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public BizKpiMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /**
     * 新注册用户计数（按角色）。
     *
     * @param roleLowerCase teacher/student/org
     */
    public void incUserRegister(String roleLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_USER_REGISTER_TOTAL, Tags.of("role", safeEnum(roleLowerCase))).increment();
    }

    /**
     * 申请沟通创建计数（按发起方）。
     *
     * @param initiatorLowerCase teacher/student/org
     */
    public void incCommApply(String initiatorLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_COMM_APPLY_TOTAL, Tags.of("initiator", safeEnum(initiatorLowerCase))).increment();
    }

    /**
     * 申请沟通通过/拒绝计数（按发起方与决策）。
     *
     * @param initiatorLowerCase teacher/student/org
     * @param decisionLowerCase  approved/rejected
     */
    public void incCommApplyDecision(String initiatorLowerCase, String decisionLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_COMM_APPLY_DECISION_TOTAL, Tags.of(
                "initiator", safeEnum(initiatorLowerCase),
                "decision", safeEnum(decisionLowerCase)
        )).increment();
    }

    /**
     * 短信验证码发送计数。
     */
    public void incSmsCodeSend() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_SMS_CODE_SEND_TOTAL).increment();
    }

    /**
     * 信息费支付金额累计（单位：分）。
     *
     * <p>注意：该指标为 Counter（单调递增），按天聚合使用 {@code increase(metric[1d])}。</p>
     */
    public void addPaymentInfoFeeAmountFen(long amountFen) {
        if (amountFen <= 0) return;
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_PAYMENT_INFO_FEE_AMOUNT_CENTS_TOTAL).increment((double) amountFen);
    }

    /**
     * 达成合作次数（按发起方）。
     *
     * <p>本项目口径：信息费订单支付成功并解锁聊天/联系方式。</p>
     */
    public void incCollaborationSuccess(String initiatorLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_COLLABORATION_SUCCESS_TOTAL, Tags.of("initiator", safeEnum(initiatorLowerCase))).increment();
    }

    /**
     * 退款次数 +1。
     */
    public void incRefund() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_REFUND_TOTAL).increment();
    }

    /**
     * 退款金额累计（单位：分）。
     */
    public void addRefundAmountFen(long amountFen) {
        if (amountFen <= 0) return;
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_REFUND_AMOUNT_CENTS_TOTAL).increment((double) amountFen);
    }

    private static String safeEnum(String v) {
        if (v == null) return "unknown";
        String s = v.trim();
        if (s.isEmpty()) return "unknown";
        return s.toLowerCase();
    }
}
