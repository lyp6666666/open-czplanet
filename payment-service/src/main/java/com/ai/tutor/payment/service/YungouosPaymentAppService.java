package com.ai.tutor.payment.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.controller.dto.PaymentOrderStatusResponse;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.utils.ThrowUtils;
import com.yungouos.pay.common.PayException;
import com.yungouos.pay.util.PaySignUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * YunGouOS 支付应用服务（面向 Controller 的编排层）
 *
 * <p>职责：
 * 1）统一下单与出码（创建/复用支付单 + 调用 YunGouOS SDK）
 * 2）支付单查询（供收银台轮询）
 * 3）回调处理（验签、二次校验、幂等更新、事件发布）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YungouosPaymentAppService {

    public static final String CONTEXT_BROKERAGE_ORDER = "BROKERAGE_ORDER";

    private final PaymentProperties paymentProperties;
    private final PaymentOrderService paymentOrderService;
    private final BrokerageOrderFacade brokerageOrderFacade;
    private final YungouosClient yungouosClient;

    public PrepayResponse prepay(PrepayRequest req, Long uid, String clientIp) {
        ThrowUtils.throwIf(Boolean.FALSE.equals(paymentProperties.getEnabled()), ErrorCode.OPERATION_ERROR, "支付功能已禁用");
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        String contextType = StringUtils.trimAllWhitespace(req.getContextType());
        Long contextId = req.getContextId();
        String channel = StringUtils.trimAllWhitespace(req.getChannel());

        ThrowUtils.throwIf(!CONTEXT_BROKERAGE_ORDER.equalsIgnoreCase(contextType), ErrorCode.PARAMS_ERROR, "暂不支持的 contextType");
        validateChannel(channel);

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

        LocalDateTime now = LocalDateTime.now();
        if (order.getExpireTime() == null || order.getExpireTime().isBefore(now) || !StringUtils.hasText(order.getPayData())) {
            String totalFeeYuan = fenToYuan(order.getAmount());
            PaymentProperties.Yungouos config = paymentProperties.getYungouos();
            boolean mock = config != null && StringUtils.hasText(config.getBaseUrl()) && config.getBaseUrl().startsWith("mock://");
            String appKey = config == null ? null : config.getAppKey();
            if (mock && !StringUtils.hasText(appKey)) {
                appKey = "TEST_KEY";
            }

            String data;
            try {
                if (PaymentChannel.WECHAT.getCode().equalsIgnoreCase(channel)) {
                    String mchId = config.getWechatMchId();
                    if (!mock) {
                        ThrowUtils.throwIf(!StringUtils.hasText(mchId), ErrorCode.OPERATION_ERROR, "缺少微信渠道商户号配置");
                    } else if (!StringUtils.hasText(mchId)) {
                        mchId = "MOCK_MCH";
                    }
                    data = yungouosClient.wechatNativePay(
                            order.getOrderNo(),
                            totalFeeYuan,
                            mchId,
                            order.getBody(),
                            config.getNativePayType(),
                            config.getAppId(),
                            buildAttach(order),
                            config.getNotifyUrl(),
                            config.getReturnUrl(),
                            appKey
                    );
                } else {
                    String mchId = config.getAlipayMchId();
                    if (!mock) {
                        ThrowUtils.throwIf(!StringUtils.hasText(mchId), ErrorCode.OPERATION_ERROR, "缺少支付宝渠道商户号配置");
                    } else if (!StringUtils.hasText(mchId)) {
                        mchId = "MOCK_MCH";
                    }
                    data = yungouosClient.alipayNativePay(
                            order.getOrderNo(),
                            totalFeeYuan,
                            mchId,
                            order.getBody(),
                            config.getNativePayType(),
                            config.getAppId(),
                            buildAttach(order),
                            config.getNotifyUrl(),
                            appKey
                    );
                }
            } catch (PayException e) {
                log.error("YunGouOS 下单失败，orderNo={}, channel={}, msg={}", order.getOrderNo(), channel, e.getMessage());
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "下单失败：" + e.getMessage());
                return null;
            }

            LocalDateTime expire = now.plus(5, ChronoUnit.MINUTES);
            String payData = JSONUtil.createObj()
                    .set("type", config.getNativePayType())
                    .set("data", data)
                    .set("channel", channel.toUpperCase())
                    .set("provider", "YUNGOUOS")
                    .toString();
            paymentOrderService.updatePayData(order.getOrderNo(), payData, expire);
            order = paymentOrderService.getByOrderNo(order.getOrderNo());
        }

        return toPrepayResponse(order);
    }

    public PaymentOrderStatusResponse getOrderStatus(String orderNo, Long uid) {
        ThrowUtils.throwIf(!StringUtils.hasText(orderNo) || uid == null, ErrorCode.PARAMS_ERROR);
        PaymentOrder order = paymentOrderService.getByOrderNo(orderNo.trim());
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(order.getUserId()), ErrorCode.NO_AUTH_ERROR);
        return toStatusResponse(order);
    }

    public String handleNotify(HttpServletRequest request) {
        if (Boolean.FALSE.equals(paymentProperties.getEnabled())) {
            return "SUCCESS";
        }
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        boolean mock = config != null && StringUtils.hasText(config.getBaseUrl()) && config.getBaseUrl().startsWith("mock://");
        String appKey = config == null ? null : config.getAppKey();
        if (mock && !StringUtils.hasText(appKey)) {
            appKey = "TEST_KEY";
        }
        ThrowUtils.throwIf(config == null || !StringUtils.hasText(appKey), ErrorCode.OPERATION_ERROR, "支付配置缺失");

        String outTradeNo = request.getParameter("out_trade_no");
        if (!StringUtils.hasText(outTradeNo)) {
            return "FAIL";
        }

        boolean signOk;
        try {
            signOk = verifyNotifySign(extractParams(request), appKey);
        } catch (Exception e) {
            log.warn("YunGouOS 回调验签异常，outTradeNo={}, msg={}", outTradeNo, e.getMessage());
            paymentOrderService.recordNotifyReceipt(outTradeNo, 0);
            return "FAIL";
        }

        if (!signOk) {
            log.warn("YunGouOS 回调验签失败，outTradeNo={}", outTradeNo);
            paymentOrderService.recordNotifyReceipt(outTradeNo, 0);
            return "FAIL";
        }

        PaymentOrder order = paymentOrderService.getByOrderNo(outTradeNo.trim());
        if (order == null) {
            log.warn("YunGouOS 回调订单不存在，outTradeNo={}", outTradeNo);
            return "FAIL";
        }

        if (StringUtils.hasText(request.getParameter("total_fee")) || StringUtils.hasText(request.getParameter("money"))) {
            String fee = StringUtils.hasText(request.getParameter("total_fee")) ? request.getParameter("total_fee") : request.getParameter("money");
            if (StringUtils.hasText(fee)) {
                boolean amountOk = amountEqualsFen(fee, order.getAmount());
                if (!amountOk) {
                    log.warn("YunGouOS 回调金额不一致，outTradeNo={}, fee={}, orderAmountFen={}", outTradeNo, fee, order.getAmount());
                    paymentOrderService.recordNotifyReceipt(outTradeNo, 1);
                    return "FAIL";
                }
            }
        }

        String payNo = firstNonBlank(request.getParameter("pay_no"), request.getParameter("transaction_id"));
        String providerOrderNo = firstNonBlank(request.getParameter("order_no"), request.getParameter("orderNo"));
        String payTime = firstNonBlank(request.getParameter("pay_time"), request.getParameter("success_time"));

        LocalDateTime successTime = parsePayTime(payTime);

        boolean ok = paymentOrderService.updateSuccessFromNotify(outTradeNo, payNo, providerOrderNo, successTime, 1);
        return ok ? "SUCCESS" : "SUCCESS";
    }

    private static Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> e : requestParams.entrySet()) {
            String key = e.getKey();
            String[] values = e.getValue();
            if (values == null || values.length == 0) {
                params.put(key, "");
                continue;
            }
            if (values.length == 1) {
                params.put(key, values[0]);
                continue;
            }
            params.put(key, String.join(",", values));
        }
        return params;
    }

    private static boolean verifyNotifySign(Map<String, String> params, String appKey) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String reqSign = params.get("sign");
        if (!StringUtils.hasText(reqSign) || !StringUtils.hasText(appKey)) {
            return false;
        }

        Map<String, Object> signParams = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            if (!StringUtils.hasText(k)) {
                continue;
            }
            if ("sign".equalsIgnoreCase(k)) {
                continue;
            }
            String v = e.getValue();
            if (!StringUtils.hasText(v)) {
                continue;
            }
            signParams.put(k, v);
        }

        String expect = PaySignUtil.createSign(signParams, appKey);
        return reqSign.trim().equalsIgnoreCase(expect);
    }

    private static void validateChannel(String channel) {
        String c = channel == null ? "" : channel.trim().toUpperCase();
        ThrowUtils.throwIf(!PaymentChannel.WECHAT.getCode().equals(c) && !PaymentChannel.ALIPAY.getCode().equals(c), ErrorCode.PARAMS_ERROR, "不支持的支付渠道");
    }

    private static String fenToYuan(Long amountFen) {
        ThrowUtils.throwIf(amountFen == null || amountFen <= 0, ErrorCode.PARAMS_ERROR);
        return new BigDecimal(amountFen).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).toString();
    }

    private static boolean amountEqualsFen(String yuan, Long amountFen) {
        try {
            BigDecimal y = new BigDecimal(yuan).setScale(2, RoundingMode.HALF_UP);
            BigDecimal fen = y.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP);
            return amountFen != null && fen.longValueExact() == amountFen;
        } catch (Exception e) {
            return false;
        }
    }

    private static String defaultSubject(BrokerageOrderPayInfo payInfo) {
        String s = payInfo == null ? null : payInfo.getSubject();
        return StringUtils.hasText(s) ? s : "中介费支付";
    }

    private static String defaultBody(BrokerageOrderPayInfo payInfo) {
        String s = payInfo == null ? null : payInfo.getBody();
        return StringUtils.hasText(s) ? s : "中介费支付";
    }

    private static String buildAttach(PaymentOrder order) {
        if (order == null) return null;
        return JSONUtil.createObj()
                .set("contextType", order.getContextType())
                .set("contextId", order.getContextId())
                .set("userId", order.getUserId())
                .toString();
    }

    private static PrepayResponse toPrepayResponse(PaymentOrder order) {
        PrepayResponse resp = new PrepayResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setAmountFen(order.getAmount());
        resp.setChannel(order.getChannel());
        resp.setExpireTime(order.getExpireTime());

        String payData = order.getPayData();
        if (StringUtils.hasText(payData) && JSONUtil.isTypeJSON(payData)) {
            try {
                cn.hutool.json.JSONObject obj = JSONUtil.parseObj(payData);
                String type = obj.getStr("type");
                String data = obj.getStr("data");
                if ("2".equals(type)) {
                    resp.setQrCodeUrl(data);
                } else {
                    resp.setCodeUrl(data);
                }
            } catch (Exception ignored) {
            }
        }
        return resp;
    }

    private static PaymentOrderStatusResponse toStatusResponse(PaymentOrder order) {
        PaymentOrderStatusResponse resp = new PaymentOrderStatusResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setAmountFen(order.getAmount());
        resp.setChannel(order.getChannel());
        resp.setSuccessTime(order.getSuccessTime());
        resp.setExpireTime(order.getExpireTime());
        return resp;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (StringUtils.hasText(v)) return v.trim();
        }
        return null;
    }

    private static LocalDateTime parsePayTime(String payTime) {
        if (!StringUtils.hasText(payTime)) {
            return LocalDateTime.now();
        }
        String s = payTime.trim();
        try {
            if (s.length() >= 19 && s.charAt(10) == ' ') {
                return LocalDateTime.parse(s.replace(" ", "T"));
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
