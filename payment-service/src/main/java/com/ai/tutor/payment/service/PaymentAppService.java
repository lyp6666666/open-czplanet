package com.ai.tutor.payment.service;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.strategy.PaymentStrategy;
import com.ai.tutor.payment.strategy.impl.WechatPaymentStrategy;
import com.ai.tutor.utils.ThrowUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 原生支付应用服务（微信/支付宝）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAppService {

    public static final String CONTEXT_BROKERAGE_ORDER = "BROKERAGE_ORDER";

    private final PaymentProperties paymentProperties;
    private final PaymentOrderService paymentOrderService;
    private final BrokerageOrderFacade brokerageOrderFacade;
    private final WechatPaymentStrategy wechatPaymentStrategy;

    public PrepayResponse prepay(PrepayRequest req, Long uid, String clientIp) {
        ThrowUtils.throwIf(Boolean.FALSE.equals(paymentProperties.getEnabled()), ErrorCode.OPERATION_ERROR, "支付功能已禁用");
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        String contextType = StringUtils.trimAllWhitespace(req.getContextType());
        Long contextId = req.getContextId();
        String channel = StringUtils.trimAllWhitespace(req.getChannel());

        ThrowUtils.throwIf(!CONTEXT_BROKERAGE_ORDER.equalsIgnoreCase(contextType), ErrorCode.PARAMS_ERROR, "暂不支持的 contextType");
        
        BrokerageOrderPayInfo payInfo = brokerageOrderFacade.getPayableOrder(contextId, uid);
        ThrowUtils.throwIf(payInfo == null, ErrorCode.NOT_FOUND_ERROR);

        PaymentOrder order = paymentOrderService.createOrReusePending(
                CONTEXT_BROKERAGE_ORDER,
                payInfo.getOrderId(),
                uid,
                channel.toUpperCase(),
                payInfo.getAmountFen(),
                defaultSubject(payInfo),
                defaultBody(payInfo),
                clientIp
        );

        // 如果是 JSAPI，将 openid 放入 extraParams
        if (StringUtils.hasText(req.getOpenid())) {
            Map<String, String> extra = new HashMap<>();
            extra.put("openid", req.getOpenid());
            order.setExtraParams(JSONUtil.toJsonStr(extra));
            // 更新订单以保存 openid
            paymentOrderService.updateById(order);
        }

        LocalDateTime now = LocalDateTime.now();
        Object payParams = null;
        
        // 即使订单已存在，如果 payData 空或者需要刷新参数，重新生成
        // 这里简化逻辑：每次请求都尝试生成签名参数，因为 JSAPI prepay_id 有有效期，但前端可能需要新的签名（timestamp变化）
        // 实际上 prepay_id 有效期 2 小时，可以复用。
        // 对于微信支付，我们调用 Strategy 生成参数
        
        PaymentStrategy strategy = getStrategy(channel);
        try {
            payParams = strategy.generatePayParams(order);
        } catch (Exception e) {
            log.error("生成支付参数失败", e);
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "生成支付参数失败：" + e.getMessage());
        }

        // 保存 payData (可选，这里存 JSON 字符串)
        LocalDateTime expire = now.plus(2, ChronoUnit.HOURS);
        paymentOrderService.updatePayData(order.getOrderNo(), JSONUtil.toJsonStr(payParams), expire);

        return toPrepayResponse(order, payParams);
    }

    private PaymentStrategy getStrategy(String channel) {
        if (PaymentChannel.WECHAT.getCode().equalsIgnoreCase(channel)) {
            return wechatPaymentStrategy;
        }
        // TODO: Alipay Strategy
        throw new IllegalArgumentException("不支持的支付渠道: " + channel);
    }

    private static String defaultSubject(BrokerageOrderPayInfo payInfo) {
        String s = payInfo == null ? null : payInfo.getSubject();
        return StringUtils.hasText(s) ? s : "服务费";
    }

    private static String defaultBody(BrokerageOrderPayInfo payInfo) {
        String s = payInfo == null ? null : payInfo.getBody();
        return StringUtils.hasText(s) ? s : "服务费";
    }

    private static PrepayResponse toPrepayResponse(PaymentOrder order, Object payParams) {
        PrepayResponse resp = new PrepayResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setAmountFen(order.getAmount());
        resp.setChannel(order.getChannel());
        resp.setExpireTime(order.getExpireTime());
        resp.setPayParams(payParams);
        return resp;
    }
}
