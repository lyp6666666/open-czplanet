package com.ai.tutor.payment.service;

import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.InternalRefundRequest;
import com.ai.tutor.payment.controller.dto.InternalRefundResponse;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.model.entity.PaymentRefund;
import com.ai.tutor.utils.ThrowUtils;
import com.yungouos.pay.common.PayException;
import com.yungouos.pay.entity.RefundOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 退款编排服务（管理端审核通过后发起原路退款）
 *
 * <p>本服务不对外暴露，Controller 会额外校验内部调用令牌。</p>
 *
 * <p>关键约束：
 * 1）仅允许对支付成功的 payment_order 发起退款；
 * 2）按 refund_request.id（requestId）做强幂等；
 * 3）支持部分退款（试课不通过退 60%）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundAppService {

    private final PaymentProperties paymentProperties;
    private final PaymentOrderService paymentOrderService;
    private final PaymentRefundService paymentRefundService;
    private final YungouosClient yungouosClient;
    private final BizKpiMetrics bizKpiMetrics;

    public InternalRefundResponse refund(InternalRefundRequest req) {
        ThrowUtils.throwIf(Boolean.FALSE.equals(paymentProperties.getEnabled()), ErrorCode.OPERATION_ERROR, "支付功能已禁用");
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR, "request 为空");
        ThrowUtils.throwIf(req.getRequestId() == null, ErrorCode.PARAMS_ERROR, "requestId 不能为空");
        ThrowUtils.throwIf(req.getRefundAmountFen() == null || req.getRefundAmountFen() <= 0, ErrorCode.PARAMS_ERROR, "refundAmountFen 非法");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getReason()), ErrorCode.PARAMS_ERROR, "reason 不能为空");

        PaymentRefund existing = paymentRefundService.getByRequestId(req.getRequestId());
        if (existing != null) {
            return InternalRefundResponse.builder().refundNo(existing.getRefundNo()).status(existing.getStatus()).build();
        }

        PaymentOrder paymentOrder = resolvePaymentOrder(req);
        ThrowUtils.throwIf(paymentOrder == null, ErrorCode.NOT_FOUND_ERROR, "未找到支付成功的支付单");
        ThrowUtils.throwIf(!PaymentStatus.SUCCESS.getCode().equals(paymentOrder.getStatus()), ErrorCode.OPERATION_ERROR, "支付单未成功，无法退款");
        ThrowUtils.throwIf(req.getRefundAmountFen() > paymentOrder.getAmount(), ErrorCode.PARAMS_ERROR, "退款金额不可大于实付金额");

        PaymentRefund refund = new PaymentRefund();
        refund.setRefundNo(cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        refund.setPaymentOrderNo(paymentOrder.getOrderNo());
        refund.setProvider("YUNGOUOS");
        refund.setRefundAmountFen(req.getRefundAmountFen());
        refund.setStatus("PENDING");
        refund.setRequestId(req.getRequestId());
        paymentRefundService.save(refund);
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：退款申请量和最终退款成功量需要拆开看，这里只在退款申请记录真正创建成功后计数一次，
             * 便于运营区分“用户申请退款变多”和“支付域真的打款成功变多”。
             */
            bizKpiMetrics.incRefundRequest("brokerage_order");
        }

        try {
            RefundOrder result = callProviderRefund(paymentOrder, refund, req.getReason().trim());
            String providerRefundNo = result == null ? null : result.getRefundNo();
            Integer refundStatus = result == null ? null : result.getRefundStatus();
            String status = (refundStatus != null && refundStatus == 1) ? "SUCCESS" : "PENDING";
            refund.setProviderRefundNo(providerRefundNo);
            refund.setStatus(status);
            refund.setFailReason(null);
            paymentRefundService.updateById(refund);
            if ("SUCCESS".equals(status) && bizKpiMetrics != null) {
                /*
                 * 中文注释：退款成功指标只在支付域收到提供方成功结果并把状态落库为 SUCCESS 后累计，
                 * 避免管理端审批、重复调用或轮询查询造成同一笔退款重复记数。
                 */
                bizKpiMetrics.incRefund();
                bizKpiMetrics.addRefundAmountFen(refund.getRefundAmountFen());
            }
        } catch (PayException e) {
            String msg = e.getMessage() == null ? "YunGouOS 退款失败" : e.getMessage();
            log.warn("YunGouOS 退款失败，orderNo={}, refundNo={}, msg={}", paymentOrder.getOrderNo(), refund.getRefundNo(), msg);
            refund.setStatus("FAILED");
            refund.setFailReason(truncate(msg));
            paymentRefundService.updateById(refund);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "退款异常" : e.getMessage();
            log.error("退款异常，orderNo={}, refundNo={}", paymentOrder.getOrderNo(), refund.getRefundNo(), e);
            refund.setStatus("FAILED");
            refund.setFailReason(truncate(msg));
            paymentRefundService.updateById(refund);
        }

        PaymentRefund latest = paymentRefundService.getByRequestId(req.getRequestId());
        return InternalRefundResponse.builder()
                .refundNo(latest == null ? refund.getRefundNo() : latest.getRefundNo())
                .status(latest == null ? refund.getStatus() : latest.getStatus())
                .build();
    }

    private PaymentOrder resolvePaymentOrder(InternalRefundRequest req) {
        if (StringUtils.hasText(req.getPaymentOrderNo())) {
            return paymentOrderService.getByOrderNo(req.getPaymentOrderNo().trim());
        }
        String contextType = req.getContextType() == null ? null : req.getContextType().trim();
        Long contextId = req.getContextId();
        ThrowUtils.throwIf(!StringUtils.hasText(contextType) || contextId == null, ErrorCode.PARAMS_ERROR, "paymentOrderNo 或 contextType/contextId 必须提供");
        return paymentOrderService.getLatestSuccessByContext(contextType, contextId);
    }

    private RefundOrder callProviderRefund(PaymentOrder paymentOrder, PaymentRefund refund, String reason) {
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        ThrowUtils.throwIf(config == null, ErrorCode.OPERATION_ERROR, "支付配置缺失");
        String baseUrl = config.getBaseUrl() == null ? null : config.getBaseUrl().trim();
        boolean mock = StringUtils.hasText(baseUrl) && baseUrl.startsWith("mock://");
        String appKey = config.getAppKey();
        if (mock && !StringUtils.hasText(appKey)) {
            appKey = "TEST_KEY";
        }
        ThrowUtils.throwIf(!StringUtils.hasText(appKey), ErrorCode.OPERATION_ERROR, "缺少 YunGouOS appKey 配置");
        ThrowUtils.throwIf(!StringUtils.hasText(paymentOrder.getChannel()), ErrorCode.OPERATION_ERROR, "支付单缺少 channel");

        String channel = paymentOrder.getChannel().trim().toUpperCase();
        String refundMoneyYuan = fenToYuan(refund.getRefundAmountFen());
        String notifyUrl = null;

        if (PaymentChannel.WECHAT.getCode().equalsIgnoreCase(channel)) {
            String mchId = config.getWechatMchId();
            if (mock && !StringUtils.hasText(mchId)) {
                mchId = "MOCK_MCH";
            }
            ThrowUtils.throwIf(!StringUtils.hasText(mchId), ErrorCode.OPERATION_ERROR, "缺少微信渠道商户号配置");
            return yungouosClient.wechatRefund(paymentOrder.getOrderNo(), mchId, refundMoneyYuan, refund.getRefundNo(), truncate(reason), notifyUrl, appKey);
        }
        if (PaymentChannel.ALIPAY.getCode().equalsIgnoreCase(channel)) {
            String mchId = config.getAlipayMchId();
            if (mock && !StringUtils.hasText(mchId)) {
                mchId = "MOCK_MCH";
            }
            ThrowUtils.throwIf(!StringUtils.hasText(mchId), ErrorCode.OPERATION_ERROR, "缺少支付宝渠道商户号配置");
            return yungouosClient.alipayRefund(paymentOrder.getOrderNo(), mchId, refundMoneyYuan, refund.getRefundNo(), truncate(reason), notifyUrl, appKey);
        }
        ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "不支持的退款渠道：" + channel);
        return null;
    }

    private static String fenToYuan(Long fen) {
        if (fen == null) {
            return "0.00";
        }
        BigDecimal v = BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        return v.setScale(2, RoundingMode.DOWN).toPlainString();
    }

    private static String truncate(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.length() <= 200) return v;
        return v.substring(0, 200);
    }
}
