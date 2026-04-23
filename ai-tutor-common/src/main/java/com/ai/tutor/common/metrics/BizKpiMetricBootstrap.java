package com.ai.tutor.common.metrics;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * 启动时预注册业务 KPI 指标，避免“没有事件就没有指标”导致 Grafana 全部显示 No data。
 *
 * <p>中文说明：这里只做零值注册，不会改动业务流程，也不会伪造任何真实业务事件。</p>
 */
@Component
public class BizKpiMetricBootstrap implements SmartInitializingSingleton {

    private final ObjectProvider<BizKpiMetrics> bizKpiMetricsProvider;

    public BizKpiMetricBootstrap(ObjectProvider<BizKpiMetrics> bizKpiMetricsProvider) {
        this.bizKpiMetricsProvider = bizKpiMetricsProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        BizKpiMetrics metrics = bizKpiMetricsProvider.getIfAvailable();
        if (metrics == null) {
            return;
        }

        registerUserMetrics(metrics);
        registerJobMetrics(metrics);
        registerCommMetrics(metrics);
        registerPaymentMetrics(metrics);
        registerChatMetrics(metrics);
        registerTrialMetrics(metrics);
        registerRefundMetrics(metrics);
    }

    private void registerUserMetrics(BizKpiMetrics metrics) {
        for (String role : new String[]{"teacher", "student", "org"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_USER_REGISTER_TOTAL, "role", role);
            metrics.registerCounter(BizKpiMetrics.METRIC_USER_LOGIN_TOTAL, "role", role);
            metrics.registerCounter(BizKpiMetrics.METRIC_PROFILE_COMPLETED_TOTAL, "role", role);
        }
        metrics.registerCounter(BizKpiMetrics.METRIC_SMS_CODE_SEND_TOTAL);
    }

    private void registerJobMetrics(BizKpiMetrics metrics) {
        for (String role : new String[]{"student", "org"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_JOB_POST_CREATED_TOTAL, "publisher_role", role);
            for (String closeReason : new String[]{"filled", "cancelled", "expired", "other"}) {
                metrics.registerCounter(BizKpiMetrics.METRIC_JOB_POST_CLOSED_TOTAL,
                        "publisher_role", role,
                        "close_reason", closeReason);
            }
        }
        for (String viewerRole : new String[]{"teacher", "student", "org"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_JOB_DETAIL_VIEW_TOTAL, "viewer_role", viewerRole);
        }
    }

    private void registerCommMetrics(BizKpiMetrics metrics) {
        for (String initiator : new String[]{"teacher", "student", "org"}) {
            for (String contextType : new String[]{"demand", "tutor", "org_posting"}) {
                metrics.registerCounter(BizKpiMetrics.METRIC_COMM_APPLY_TOTAL,
                        "initiator", initiator,
                        "context_type", contextType);
            }
            for (String decision : new String[]{"approved", "rejected"}) {
                metrics.registerCounter(BizKpiMetrics.METRIC_COMM_APPLY_DECISION_TOTAL,
                        "initiator", initiator,
                        "decision", decision);
            }
        }
        for (String viewerRole : new String[]{"teacher", "student", "org"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_COMM_APPLY_DETAIL_VIEW_TOTAL, "viewer_role", viewerRole);
        }
    }

    private void registerPaymentMetrics(BizKpiMetrics metrics) {
        for (String bizType : new String[]{"info_fee", "brokerage_order", "lesson_payment_order"}) {
            for (String channel : new String[]{"wechat", "alipay", "other"}) {
                metrics.registerCounter(BizKpiMetrics.METRIC_PAYMENT_ORDER_CREATED_TOTAL,
                        "biz_type", bizType,
                        "channel", channel);
                metrics.registerCounter(BizKpiMetrics.METRIC_PAYMENT_SUCCESS_TOTAL,
                        "biz_type", bizType,
                        "channel", channel);
            }
        }
        metrics.registerCounter(BizKpiMetrics.METRIC_PAYMENT_INFO_FEE_AMOUNT_CENTS_TOTAL);
    }

    private void registerChatMetrics(BizKpiMetrics metrics) {
        for (String unlockReason : new String[]{"payment_success", "manual_reopen"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_CHAT_UNLOCK_TOTAL, "unlock_reason", unlockReason);
        }
        for (String reason : new String[]{"course_status_not_updated", "update_failed", "consumer_failed", "retry_exhausted", "other"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_CHAT_UNLOCK_FAILED_TOTAL, "reason", reason);
        }
        for (String entryType : new String[]{"created", "existing", "start_chat"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_CHAT_ROOM_ENTER_TOTAL, "entry_type", entryType);
        }
        for (String messageType : new String[]{"text", "image", "system", "other"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_CHAT_MESSAGE_SENT_TOTAL, "message_type", messageType);
        }
        for (String reason : new String[]{"access_denied", "persist_error", "push_error", "other"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_CHAT_MESSAGE_FAILED_TOTAL, "reason", reason);
        }
        for (String eventType : new String[]{"message", "application", "delivery", "read", "presence", "other"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_CHAT_REALTIME_DELIVERED_TOTAL, "event_type", eventType);
        }
        for (String initiator : new String[]{"teacher", "student", "org"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_COLLABORATION_SUCCESS_TOTAL, "initiator", initiator);
        }
    }

    private void registerTrialMetrics(BizKpiMetrics metrics) {
        for (String initiator : new String[]{"teacher", "student"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_PROPOSAL_CREATED_TOTAL, "initiator", initiator);
        }
        for (String decision : new String[]{"accepted", "rejected"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_PROPOSAL_DECISION_TOTAL, "decision", decision);
        }
        metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_PROPOSAL_EXPIRED_TOTAL);
        metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_SCHEDULED_TOTAL);
        metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_RESCHEDULE_CREATED_TOTAL);
        for (String decision : new String[]{"accepted", "rejected", "invalidated"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_RESCHEDULE_DECISION_TOTAL, "decision", decision);
        }
        for (String cancelBy : new String[]{"teacher", "student", "system"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_CANCEL_TOTAL, "cancel_by", cancelBy);
        }
        metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_FINISHED_TOTAL);
        for (String decision : new String[]{"passed", "failed"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_TRIAL_DECISION_TOTAL, "decision", decision);
        }
        metrics.registerCounter(BizKpiMetrics.METRIC_WEEKLY_SCHEDULE_SUBMITTED_TOTAL);
        metrics.registerCounter(BizKpiMetrics.METRIC_WEEKLY_SCHEDULE_TIMEOUT_TOTAL);
    }

    private void registerRefundMetrics(BizKpiMetrics metrics) {
        for (String refundType : new String[]{"trial_info_fee", "chat_refund", "brokerage_order", "other"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_REFUND_REQUEST_TOTAL, "refund_type", refundType);
        }
        for (String decision : new String[]{"approved", "rejected"}) {
            metrics.registerCounter(BizKpiMetrics.METRIC_REFUND_REVIEW_TOTAL, "decision", decision);
        }
        metrics.registerCounter(BizKpiMetrics.METRIC_REFUND_TOTAL);
        metrics.registerCounter(BizKpiMetrics.METRIC_REFUND_AMOUNT_CENTS_TOTAL);
    }
}
