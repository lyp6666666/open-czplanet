package com.ai.tutor.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;

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
    public static final String METRIC_USER_LOGIN_TOTAL = "ai_tutor_biz_user_login_total";
    public static final String METRIC_PROFILE_COMPLETED_TOTAL = "ai_tutor_biz_profile_completed_total";
    public static final String METRIC_COMM_APPLY_TOTAL = "ai_tutor_biz_comm_apply_total";
    public static final String METRIC_COMM_APPLY_DECISION_TOTAL = "ai_tutor_biz_comm_apply_decision_total";
    public static final String METRIC_COMM_APPLY_DETAIL_VIEW_TOTAL = "ai_tutor_biz_comm_apply_detail_view_total";
    public static final String METRIC_SMS_CODE_SEND_TOTAL = "ai_tutor_biz_sms_code_send_total";
    public static final String METRIC_JOB_POST_CREATED_TOTAL = "ai_tutor_biz_job_post_created_total";
    public static final String METRIC_JOB_POST_CLOSED_TOTAL = "ai_tutor_biz_job_post_closed_total";
    public static final String METRIC_JOB_DETAIL_VIEW_TOTAL = "ai_tutor_biz_job_detail_view_total";
    public static final String METRIC_PAYMENT_ORDER_CREATED_TOTAL = "ai_tutor_biz_payment_order_created_total";
    public static final String METRIC_PAYMENT_SUCCESS_TOTAL = "ai_tutor_biz_payment_success_total";
    public static final String METRIC_PAYMENT_INFO_FEE_AMOUNT_CENTS_TOTAL = "ai_tutor_biz_payment_info_fee_amount_cents_total";
    public static final String METRIC_CHAT_UNLOCK_TOTAL = "ai_tutor_biz_chat_unlock_total";
    public static final String METRIC_CHAT_UNLOCK_FAILED_TOTAL = "ai_tutor_biz_chat_unlock_failed_total";
    public static final String METRIC_CHAT_ROOM_ENTER_TOTAL = "ai_tutor_biz_chat_room_enter_total";
    public static final String METRIC_CHAT_MESSAGE_SENT_TOTAL = "ai_tutor_biz_chat_message_sent_total";
    public static final String METRIC_CHAT_MESSAGE_FAILED_TOTAL = "ai_tutor_biz_chat_message_failed_total";
    public static final String METRIC_CHAT_REALTIME_DELIVERED_TOTAL = "ai_tutor_biz_chat_realtime_delivered_total";
    public static final String METRIC_COLLABORATION_SUCCESS_TOTAL = "ai_tutor_biz_collaboration_success_total";
    public static final String METRIC_TRIAL_PROPOSAL_CREATED_TOTAL = "ai_tutor_biz_trial_proposal_created_total";
    public static final String METRIC_TRIAL_PROPOSAL_DECISION_TOTAL = "ai_tutor_biz_trial_proposal_decision_total";
    public static final String METRIC_TRIAL_PROPOSAL_EXPIRED_TOTAL = "ai_tutor_biz_trial_proposal_expired_total";
    public static final String METRIC_TRIAL_SCHEDULED_TOTAL = "ai_tutor_biz_trial_scheduled_total";
    public static final String METRIC_TRIAL_RESCHEDULE_CREATED_TOTAL = "ai_tutor_biz_trial_reschedule_created_total";
    public static final String METRIC_TRIAL_RESCHEDULE_DECISION_TOTAL = "ai_tutor_biz_trial_reschedule_decision_total";
    public static final String METRIC_TRIAL_CANCEL_TOTAL = "ai_tutor_biz_trial_cancel_total";
    public static final String METRIC_TRIAL_FINISHED_TOTAL = "ai_tutor_biz_trial_finished_total";
    public static final String METRIC_TRIAL_DECISION_TOTAL = "ai_tutor_biz_trial_decision_total";
    public static final String METRIC_WEEKLY_SCHEDULE_SUBMITTED_TOTAL = "ai_tutor_biz_weekly_schedule_submitted_total";
    public static final String METRIC_WEEKLY_SCHEDULE_TIMEOUT_TOTAL = "ai_tutor_biz_weekly_schedule_timeout_total";
    public static final String METRIC_REFUND_REQUEST_TOTAL = "ai_tutor_biz_refund_request_total";
    public static final String METRIC_REFUND_REVIEW_TOTAL = "ai_tutor_biz_refund_review_total";
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
     * 登录成功计数（按角色）。
     *
     * @param roleLowerCase teacher/student/org
     */
    public void incUserLogin(String roleLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_USER_LOGIN_TOTAL, Tags.of("role", safeEnum(roleLowerCase))).increment();
    }

    /**
     * 资料首次达到业务准入门槛计数（按角色）。
     *
     * @param roleLowerCase teacher/student/org
     */
    public void incProfileCompleted(String roleLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_PROFILE_COMPLETED_TOTAL, Tags.of("role", safeEnum(roleLowerCase))).increment();
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
     * 申请沟通创建计数（按发起方与上下文）。
     *
     * @param initiatorLowerCase teacher/student/org
     * @param contextTypeLowerCase demand/tutor/org_posting
     */
    public void incCommApply(String initiatorLowerCase, String contextTypeLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_COMM_APPLY_TOTAL, Tags.of(
                "initiator", safeEnum(initiatorLowerCase),
                "context_type", safeEnum(contextTypeLowerCase)
        )).increment();
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
     * 申请详情查看计数（按查看方角色）。
     *
     * @param viewerRoleLowerCase teacher/student/org
     */
    public void incCommApplyDetailView(String viewerRoleLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_COMM_APPLY_DETAIL_VIEW_TOTAL, Tags.of("viewer_role", safeEnum(viewerRoleLowerCase))).increment();
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
     * 需求发布成功计数。
     *
     * @param publisherRoleLowerCase student/org
     */
    public void incJobPostCreated(String publisherRoleLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_JOB_POST_CREATED_TOTAL, Tags.of("publisher_role", safeEnum(publisherRoleLowerCase))).increment();
    }

    /**
     * 需求关闭计数。
     *
     * @param publisherRoleLowerCase student/org
     * @param closeReasonLowerCase   filled/cancelled/expired/other
     */
    public void incJobPostClosed(String publisherRoleLowerCase, String closeReasonLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_JOB_POST_CLOSED_TOTAL, Tags.of(
                "publisher_role", safeEnum(publisherRoleLowerCase),
                "close_reason", safeEnum(closeReasonLowerCase)
        )).increment();
    }

    /**
     * 需求详情查看计数。
     *
     * @param viewerRoleLowerCase teacher/student/org
     */
    public void incJobDetailView(String viewerRoleLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_JOB_DETAIL_VIEW_TOTAL, Tags.of("viewer_role", safeEnum(viewerRoleLowerCase))).increment();
    }

    /**
     * 信息费待支付订单创建计数。
     *
     * @param bizTypeLowerCase info_fee/brokerage_order/lesson_payment_order
     * @param channelLowerCase wechat/alipay/other
     */
    public void incPaymentOrderCreated(String bizTypeLowerCase, String channelLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_PAYMENT_ORDER_CREATED_TOTAL, Tags.of(
                "biz_type", safeEnum(bizTypeLowerCase),
                "channel", safeEnum(channelLowerCase)
        )).increment();
    }

    /**
     * 信息费支付成功计数。
     *
     * @param bizTypeLowerCase info_fee/brokerage_order/lesson_payment_order
     * @param channelLowerCase wechat/alipay/other
     */
    public void incPaymentSuccess(String bizTypeLowerCase, String channelLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_PAYMENT_SUCCESS_TOTAL, Tags.of(
                "biz_type", safeEnum(bizTypeLowerCase),
                "channel", safeEnum(channelLowerCase)
        )).increment();
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
     * 支付后聊天解锁成功计数。
     *
     * @param unlockReasonLowerCase payment_success/manual_reopen
     */
    public void incChatUnlock(String unlockReasonLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_CHAT_UNLOCK_TOTAL, Tags.of("unlock_reason", safeEnum(unlockReasonLowerCase))).increment();
    }

    /**
     * 支付成功后聊天解锁失败计数。
     *
     * @param reasonLowerCase update_failed/consumer_failed/retry_exhausted/other
     */
    public void incChatUnlockFailed(String reasonLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_CHAT_UNLOCK_FAILED_TOTAL, Tags.of("reason", safeEnum(reasonLowerCase))).increment();
    }

    /**
     * 聊天房间进入计数。
     *
     * @param entryTypeLowerCase created/existing/start_chat
     */
    public void incChatRoomEnter(String entryTypeLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_CHAT_ROOM_ENTER_TOTAL, Tags.of("entry_type", safeEnum(entryTypeLowerCase))).increment();
    }

    /**
     * 聊天消息发送成功计数。
     *
     * @param messageTypeLowerCase text/image/system/other
     */
    public void incChatMessageSent(String messageTypeLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_CHAT_MESSAGE_SENT_TOTAL, Tags.of("message_type", safeEnum(messageTypeLowerCase))).increment();
    }

    /**
     * 聊天消息发送失败计数。
     *
     * @param reasonLowerCase access_denied/persist_error/push_error/other
     */
    public void incChatMessageFailed(String reasonLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_CHAT_MESSAGE_FAILED_TOTAL, Tags.of("reason", safeEnum(reasonLowerCase))).increment();
    }

    /**
     * SSE 实时事件成功投递计数。
     *
     * @param eventTypeLowerCase message/application/delivery/read/presence/other
     */
    public void incChatRealtimeDelivered(String eventTypeLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_CHAT_REALTIME_DELIVERED_TOTAL, Tags.of("event_type", safeEnum(eventTypeLowerCase))).increment();
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
     * 试课合作提案创建计数。
     *
     * @param initiatorLowerCase teacher/student
     */
    public void incTrialProposalCreated(String initiatorLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_PROPOSAL_CREATED_TOTAL, Tags.of("initiator", safeEnum(initiatorLowerCase))).increment();
    }

    /**
     * 试课合作提案处理计数。
     *
     * @param decisionLowerCase accepted/rejected
     */
    public void incTrialProposalDecision(String decisionLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_PROPOSAL_DECISION_TOTAL, Tags.of("decision", safeEnum(decisionLowerCase))).increment();
    }

    /**
     * 试课合作提案过期计数。
     */
    public void incTrialProposalExpired() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_PROPOSAL_EXPIRED_TOTAL).increment();
    }

    /**
     * 试课安排成功计数。
     */
    public void incTrialScheduled() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_SCHEDULED_TOTAL).increment();
    }

    /**
     * 试课改期提案创建计数。
     */
    public void incTrialRescheduleCreated() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_RESCHEDULE_CREATED_TOTAL).increment();
    }

    /**
     * 试课改期处理计数。
     *
     * @param decisionLowerCase accepted/rejected/invalidated
     */
    public void incTrialRescheduleDecision(String decisionLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_RESCHEDULE_DECISION_TOTAL, Tags.of("decision", safeEnum(decisionLowerCase))).increment();
    }

    /**
     * 试课取消计数。
     *
     * @param cancelByLowerCase teacher/student/system
     */
    public void incTrialCancel(String cancelByLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_CANCEL_TOTAL, Tags.of("cancel_by", safeEnum(cancelByLowerCase))).increment();
    }

    /**
     * 试课结束进入学生决策阶段计数。
     */
    public void incTrialFinished() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_FINISHED_TOTAL).increment();
    }

    /**
     * 试课后学生决策计数。
     *
     * @param decisionLowerCase passed/failed
     */
    public void incTrialDecision(String decisionLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_TRIAL_DECISION_TOTAL, Tags.of("decision", safeEnum(decisionLowerCase))).increment();
    }

    /**
     * 正式课表提交成功计数。
     */
    public void incWeeklyScheduleSubmitted() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_WEEKLY_SCHEDULE_SUBMITTED_TOTAL).increment();
    }

    /**
     * 正式课表超时计数。
     */
    public void incWeeklyScheduleTimeout() {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_WEEKLY_SCHEDULE_TIMEOUT_TOTAL).increment();
    }

    /**
     * 退款申请创建计数。
     *
     * @param refundTypeLowerCase trial_info_fee/chat_refund/other
     */
    public void incRefundRequest(String refundTypeLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_REFUND_REQUEST_TOTAL, Tags.of("refund_type", safeEnum(refundTypeLowerCase))).increment();
    }

    /**
     * 管理端退款审批计数。
     *
     * @param decisionLowerCase approved/rejected
     */
    public void incRefundReview(String decisionLowerCase) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        r.counter(METRIC_REFUND_REVIEW_TOTAL, Tags.of("decision", safeEnum(decisionLowerCase))).increment();
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

    /**
     * 预注册业务指标，确保没有真实事件时 Prometheus 也能暴露 0 值样本，避免 Grafana 直接显示 No data。
     *
     * <p>中文说明：这里只做“注册”，不会增加计数，不影响任何真实业务口径。</p>
     *
     * @param metricName 指标名
     * @param keyValues  Micrometer tag 键值对
     */
    public void registerCounter(String metricName, String... keyValues) {
        MeterRegistry r = meterRegistryProvider.getIfAvailable();
        if (r == null) return;
        if (keyValues == null || keyValues.length == 0) {
            r.counter(metricName);
            return;
        }
        if ((keyValues.length & 1) == 1) {
            throw new IllegalArgumentException("keyValues must be even length: " + Arrays.toString(keyValues));
        }
        r.counter(metricName, Tags.of(keyValues));
    }

    private static String safeEnum(String v) {
        if (v == null) return "unknown";
        String s = v.trim();
        if (s.isEmpty()) return "unknown";
        return s.toLowerCase();
    }
}
