package com.ai.tutor.payment.service;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.common.integration.LessonPaymentPayInfo;
import com.ai.tutor.common.security.IdentitySignatureUtils;
import com.ai.tutor.payment.integration.feign.AppointmentLessonPaymentFeignClient;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.payment.integration.feign.ImBrokerageOrderFeignClient;
import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.controller.dto.PaymentOrderStatusResponse;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.utils.ThrowUtils;
import com.yungouos.pay.entity.PayOrder;
import com.yungouos.pay.common.PayException;
import com.yungouos.pay.util.PaySignUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public static final String CONTEXT_LESSON_PAYMENT_ORDER = "LESSON_PAYMENT_ORDER";
    private static final String PAY_NOTIFY = "PAY_NOTIFY";
    private static final String PAY_FINALIZE = "PAY_FINALIZE";
    private static final String PAY_QUERY = "PAY_QUERY";

    private final PaymentProperties paymentProperties;
    private final PaymentOrderService paymentOrderService;
    private final BrokerageOrderFacade brokerageOrderFacade;
    private final YungouosClient yungouosClient;
    private final ImBrokerageOrderFeignClient imBrokerageOrderFeignClient;
    private final AppointmentLessonPaymentFeignClient appointmentLessonPaymentFeignClient;
    private final IdentitySignatureUtils identitySignatureUtils;

    public PrepayResponse prepay(PrepayRequest req, Long uid, String clientIp) {
        ThrowUtils.throwIf(Boolean.FALSE.equals(paymentProperties.getEnabled()), ErrorCode.OPERATION_ERROR, "支付功能已禁用");
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        String contextType = StringUtils.trimAllWhitespace(req.getContextType());
        Long contextId = req.getContextId();
        String channel = StringUtils.trimAllWhitespace(req.getChannel());

        validateChannel(channel);

        PayableInfo payInfo = resolvePayableInfo(contextType, contextId, uid);

        PaymentOrder order = paymentOrderService.createOrReusePending(
                payInfo.contextType(),
                payInfo.orderId(),
                uid,
                channel.toUpperCase(),
                payInfo.amountFen(),
                payInfo.subject(),
                payInfo.body(),
                clientIp
        );
        LocalDateTime now = LocalDateTime.now();
        if (order.getExpireTime() == null || order.getExpireTime().isBefore(now) || !StringUtils.hasText(order.getPayData())) {
            String totalFeeYuan = fenToYuan(order.getAmount());
            PaymentProperties.Yungouos config = paymentProperties.getYungouos();
            ThrowUtils.throwIf(config == null, ErrorCode.OPERATION_ERROR, "支付配置缺失");
            String baseUrl = config.getBaseUrl() == null ? null : config.getBaseUrl().trim();
            boolean mock = StringUtils.hasText(baseUrl) && baseUrl.startsWith("mock://");
            String appKey = config.getAppKey();
            if (mock && !StringUtils.hasText(appKey)) {
                appKey = "TEST_KEY";
            }
            if (!mock) {
                ThrowUtils.throwIf(!StringUtils.hasText(appKey), ErrorCode.OPERATION_ERROR, "缺少 YunGouOS appKey 配置");
            }
            ThrowUtils.throwIf(!StringUtils.hasText(config.getAppId()), ErrorCode.OPERATION_ERROR, "缺少 YunGouOS appId 配置");
            ThrowUtils.throwIf(!StringUtils.hasText(config.getNotifyUrl()), ErrorCode.OPERATION_ERROR, "缺少 YunGouOS notifyUrl 配置");
            String nativePayType = config.getNativePayType() == null ? null : config.getNativePayType().trim();
            if (!"1".equals(nativePayType) && !"2".equals(nativePayType)) {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "YunGouOS nativePayType 必须为 1 或 2");
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
                            nativePayType,
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
                            nativePayType,
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
                    .set("type", nativePayType)
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
        order = trySyncPaidFromProvider(order);
        tryFinalizeBusiness(order);
        return toStatusResponse(order);
    }

    public String handleNotify(HttpServletRequest request) {
        if (Boolean.FALSE.equals(paymentProperties.getEnabled())) {
            return "SUCCESS";
        }
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        if (config == null) {
            log.warn("YunGouOS 回调处理失败：缺少配置 payment.yungouos");
            return "FAIL";
        }
        String baseUrl = config.getBaseUrl() == null ? null : config.getBaseUrl().trim();
        boolean mock = StringUtils.hasText(baseUrl) && baseUrl.startsWith("mock://");
        String appKey = config.getAppKey();
        if (mock && !StringUtils.hasText(appKey)) {
            appKey = "TEST_KEY";
        }
        if (!StringUtils.hasText(appKey)) {
            log.warn("YunGouOS 回调处理失败：缺少 appKey");
            return "FAIL";
        }

        Map<String, String> params = extractParams(request);
        String outTradeNo = params.get("out_trade_no");
        log.info("{} received provider=YUNGOUOS orderNo={} remoteAddr={}",
                PAY_NOTIFY, trimToNull(outTradeNo), request.getRemoteAddr());
        if (!StringUtils.hasText(outTradeNo)) {
            log.warn("{} failed reason=missing_order_no provider=YUNGOUOS remoteAddr={}",
                    PAY_NOTIFY, request.getRemoteAddr());
            return "FAIL";
        }

        boolean signOk;
        try {
            signOk = verifyNotifySign(params, appKey);
        } catch (Exception e) {
            log.warn("{} failed reason=verify_exception provider=YUNGOUOS orderNo={} msg={}", PAY_NOTIFY, outTradeNo, e.getMessage());
            log.warn("YunGouOS 回调验签异常，outTradeNo={}, msg={}", outTradeNo, e.getMessage());
            paymentOrderService.recordNotifyReceipt(outTradeNo, 0);
            return "FAIL";
        }

        if (!signOk) {
            log.warn("{} failed reason=verify_failed provider=YUNGOUOS orderNo={}", PAY_NOTIFY, outTradeNo);
            log.warn("YunGouOS 回调验签失败，outTradeNo={}", outTradeNo);
            paymentOrderService.recordNotifyReceipt(outTradeNo, 0);
            return "FAIL";
        }

        PaymentOrder order = paymentOrderService.getByOrderNo(outTradeNo.trim());
        if (order == null) {
            log.warn("{} failed reason=order_missing provider=YUNGOUOS orderNo={}", PAY_NOTIFY, outTradeNo);
            log.warn("YunGouOS 回调订单不存在，outTradeNo={}", outTradeNo);
            return "FAIL";
        }

        if (StringUtils.hasText(request.getParameter("total_fee")) || StringUtils.hasText(request.getParameter("money"))) {
            String fee = StringUtils.hasText(request.getParameter("total_fee")) ? request.getParameter("total_fee") : request.getParameter("money");
            if (StringUtils.hasText(fee)) {
                boolean amountOk = amountEqualsFen(fee, order.getAmount());
                if (!amountOk) {
                    log.warn("{} failed reason=amount_mismatch orderNo={} feeYuan={} orderAmountFen={}",
                            PAY_NOTIFY, outTradeNo, fee, order.getAmount());
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
        PaymentOrder updated = paymentOrderService.getByOrderNo(outTradeNo.trim());
        log.info("{} success provider=YUNGOUOS orderNo={} updateResult={} status={} transactionId={} providerOrderNo={}",
                PAY_NOTIFY, outTradeNo, ok, updated == null ? null : updated.getStatus(), payNo, providerOrderNo);
        tryFinalizeBusiness(updated);
        return ok ? "SUCCESS" : "SUCCESS";
    }

    public String handleReturn(HttpServletRequest request) {
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        String target = config == null ? null : config.getReturnPageUrl();
        if (!StringUtils.hasText(target)) {
            target = "http://localhost:5173/";
        }

        if (Boolean.FALSE.equals(paymentProperties.getEnabled())) {
            return renderRedirectHtml(target);
        }
        if (config == null) {
            return renderRedirectHtml(target);
        }
        String baseUrl = config.getBaseUrl() == null ? null : config.getBaseUrl().trim();
        boolean mock = StringUtils.hasText(baseUrl) && baseUrl.startsWith("mock://");
        String appKey = config.getAppKey();
        if (mock && !StringUtils.hasText(appKey)) {
            appKey = "TEST_KEY";
        }

        Map<String, String> params = extractParams(request);
        String outTradeNo = firstNonBlank(params.get("outTradeNo"), params.get("out_trade_no"));
        if (!StringUtils.hasText(outTradeNo)) {
            return renderRedirectHtml(target);
        }

        boolean signOk = false;
        try {
            signOk = verifyNotifySign(params, appKey);
        } catch (Exception ignored) {
        }
        if (!signOk && !mock) {
            return renderRedirectHtml(appendQuery(target, "pay", "invalid_sign"));
        }

        String code = params.get("code");
        if (!"1".equals(StringUtils.trimAllWhitespace(code))) {
            return renderRedirectHtml(appendQuery(target, "pay", "failed"));
        }

        PaymentOrder order = paymentOrderService.getByOrderNo(outTradeNo.trim());
        if (order == null) {
            return renderRedirectHtml(appendQuery(target, "pay", "order_not_found"));
        }

        String fee = firstNonBlank(params.get("money"), params.get("total_fee"));
        if (StringUtils.hasText(fee)) {
            boolean amountOk = amountEqualsFen(fee, order.getAmount());
            if (!amountOk) {
                return renderRedirectHtml(appendQuery(target, "pay", "amount_mismatch"));
            }
        }

        String payNo = firstNonBlank(params.get("payNo"), params.get("pay_no"), params.get("transaction_id"));
        String providerOrderNo = firstNonBlank(params.get("orderNo"), params.get("order_no"));
        String payTime = params.get("time");
        LocalDateTime successTime = parsePayTime(payTime);

        paymentOrderService.updateSuccessFromNotify(outTradeNo, payNo, providerOrderNo, successTime, signOk ? 1 : 0);
        PaymentOrder updated = paymentOrderService.getByOrderNo(outTradeNo.trim());
        tryFinalizeBusiness(updated);

        return renderRedirectHtml(appendQuery(target, "orderNo", outTradeNo.trim()));
    }

    private void tryFinalizeBusiness(PaymentOrder order) {
        if (order == null) {
            return;
        }
        if (!PaymentStatus.SUCCESS.getCode().equals(order.getStatus())) {
            return;
        }
        Integer sent = order.getEventSent();
        if (sent != null && sent == 1) {
            return;
        }
        String contextType = order.getContextType() == null ? "" : order.getContextType().trim().toUpperCase();
        if (!CONTEXT_BROKERAGE_ORDER.equals(contextType) && !CONTEXT_LESSON_PAYMENT_ORDER.equals(contextType)) {
            return;
        }
        if (order.getContextId() == null) {
            return;
        }

        String orderNo = order.getOrderNo();
        long ts = System.currentTimeMillis();
        long uid = 0L;
        int role = 0;
        String path = CONTEXT_LESSON_PAYMENT_ORDER.equals(contextType)
                ? "/internal/facade/lesson-payments/payment-success"
                : "/internal/facade/payment/success";
        String sign = identitySignatureUtils.sign(uid, role, ts, "POST", path);

        PaymentSuccessEvent event = buildPaymentSuccessEvent(order);

        try {
            log.info("{} start orderNo={} contextType={} contextId={} status={}",
                    PAY_FINALIZE, orderNo, order.getContextType(), order.getContextId(), order.getStatus());
            BaseResponse<Boolean> resp;
            if (CONTEXT_LESSON_PAYMENT_ORDER.equals(contextType)) {
                resp = appointmentLessonPaymentFeignClient.onPaymentSuccess(event);
            } else {
                resp = imBrokerageOrderFeignClient.onPaymentSuccess(
                        String.valueOf(uid),
                        String.valueOf(role),
                        String.valueOf(ts),
                        sign,
                        event
                );
            }
            if (resp != null && resp.getCode() == ErrorCode.SUCCESS.getCode() && Boolean.TRUE.equals(resp.getData())) {
                paymentOrderService.markEventSent(orderNo);
                log.info("{} success orderNo={} contextId={}", PAY_FINALIZE, orderNo, order.getContextId());
                return;
            }
            String msg = resp == null ? "null response" : String.valueOf(resp.getMessage());
            paymentOrderService.markEventSendFailed(orderNo, "im finalize failed: " + msg);
            log.warn("{} failed orderNo={} contextId={} msg={}", PAY_FINALIZE, orderNo, order.getContextId(), msg);
        } catch (Exception e) {
            paymentOrderService.markEventSendFailed(orderNo, "im finalize exception: " + e.getMessage());
            log.warn("{} failed orderNo={} contextId={} msg={}", PAY_FINALIZE, orderNo, order.getContextId(), e.getMessage());
        }
    }

    private PaymentOrder trySyncPaidFromProvider(PaymentOrder order) {
        if (order == null || !PaymentStatus.PENDING.getCode().equals(order.getStatus())) {
            return order;
        }
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        if (config == null || !StringUtils.hasText(config.getAppKey())) {
            return order;
        }
        String mchId = resolveMchId(order.getChannel(), config);
        if (!StringUtils.hasText(mchId)) {
            return order;
        }
        try {
            log.info("{} start provider=YUNGOUOS orderNo={} channel={}", PAY_QUERY, order.getOrderNo(), order.getChannel());
            PayOrder providerOrder = yungouosClient.getOrderInfoByOutTradeNo(order.getOrderNo(), mchId, config.getAppKey());
            if (providerOrder == null || providerOrder.getPayStatus() != 1) {
                return order;
            }
            if (StringUtils.hasText(providerOrder.getMoney()) && !amountEqualsFen(providerOrder.getMoney(), order.getAmount())) {
                log.warn("{} failed reason=amount_mismatch provider=YUNGOUOS orderNo={} feeYuan={} orderAmountFen={}",
                        PAY_QUERY, order.getOrderNo(), providerOrder.getMoney(), order.getAmount());
                return order;
            }
            paymentOrderService.updateSuccessFromProviderQuery(
                    order.getOrderNo(),
                    trimToNull(providerOrder.getPayNo()),
                    trimToNull(providerOrder.getOrderNo()),
                    null
            );
            PaymentOrder refreshed = paymentOrderService.getByOrderNo(order.getOrderNo());
            log.info("{} success provider=YUNGOUOS orderNo={} status={} transactionId={} providerOrderNo={}",
                    PAY_QUERY,
                    order.getOrderNo(),
                    refreshed == null ? null : refreshed.getStatus(),
                    refreshed == null ? null : refreshed.getTransactionId(),
                    refreshed == null ? null : refreshed.getProviderOrderNo());
            return refreshed == null ? order : refreshed;
        } catch (Exception e) {
            log.warn("{} failed reason=query_exception provider=YUNGOUOS orderNo={} msg={}", PAY_QUERY, order.getOrderNo(), e.getMessage());
            return order;
        }
    }

    private static String resolveMchId(String channel, PaymentProperties.Yungouos config) {
        if (!StringUtils.hasText(channel) || config == null) {
            return null;
        }
        if (PaymentChannel.WECHAT.getCode().equalsIgnoreCase(channel)) {
            return trimToNull(config.getWechatMchId());
        }
        if (PaymentChannel.ALIPAY.getCode().equalsIgnoreCase(channel)) {
            return trimToNull(config.getAlipayMchId());
        }
        return null;
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

    private static String appendQuery(String baseUrl, String key, String value) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(key) || value == null) {
            return baseUrl;
        }
        String sep = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + sep + urlEncode(key) + "=" + urlEncode(value);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String renderRedirectHtml(String targetUrl) {
        String safe = targetUrl == null ? "" : targetUrl;
        String escaped = escapeHtml(safe);
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta http-equiv=\"refresh\" content=\"0;url=" + escaped + "\">"
                + "</head><body>"
                + "<a href=\"" + escaped + "\">Continue</a>"
                + "</body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') out.append("&amp;");
            else if (c == '<') out.append("&lt;");
            else if (c == '>') out.append("&gt;");
            else if (c == '"') out.append("&quot;");
            else if (c == '\'') out.append("&#39;");
            else out.append(c);
        }
        return out.toString();
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

    private PayableInfo resolvePayableInfo(String contextType, Long contextId, Long uid) {
        if (CONTEXT_BROKERAGE_ORDER.equalsIgnoreCase(contextType)) {
            BrokerageOrderPayInfo payInfo = brokerageOrderFacade.getPayableOrder(contextId, uid);
            ThrowUtils.throwIf(payInfo == null, ErrorCode.NOT_FOUND_ERROR);
            return new PayableInfo(CONTEXT_BROKERAGE_ORDER, payInfo.getOrderId(), payInfo.getAmountFen(), defaultSubject(payInfo), defaultBody(payInfo));
        }
        if (CONTEXT_LESSON_PAYMENT_ORDER.equalsIgnoreCase(contextType)) {
            BaseResponse<LessonPaymentPayInfo> resp = appointmentLessonPaymentFeignClient.getPayableOrder(contextId, uid);
            ThrowUtils.throwIf(resp == null || resp.getCode() != ErrorCode.SUCCESS.getCode() || resp.getData() == null,
                    ErrorCode.OPERATION_ERROR,
                    resp == null ? "获取课节支付单失败" : resp.getMessage());
            LessonPaymentPayInfo payInfo = resp.getData();
            return new PayableInfo(CONTEXT_LESSON_PAYMENT_ORDER, payInfo.getOrderId(), payInfo.getAmountFen(),
                    StringUtils.hasText(payInfo.getSubject()) ? payInfo.getSubject() : "课后支付",
                    StringUtils.hasText(payInfo.getBody()) ? payInfo.getBody() : "课时费");
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "暂不支持的 contextType");
        return null;
    }

    private static PaymentSuccessEvent buildPaymentSuccessEvent(PaymentOrder order) {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setOrderNo(order.getOrderNo());
        event.setUserId(order.getUserId());
        event.setAmount(order.getAmount());
        event.setContextId(order.getContextId());
        event.setContextType(order.getContextType());
        event.setTransactionId(order.getTransactionId());
        event.setSuccessTime(order.getSuccessTime());
        event.setChannel(order.getChannel());
        event.setProvider(order.getProvider());
        event.setProviderOrderNo(order.getProviderOrderNo());
        return event;
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

    private record PayableInfo(String contextType, Long orderId, Long amountFen, String subject, String body) {
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

    private static String trimToNull(String val) {
        return StringUtils.hasText(val) ? val.trim() : null;
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
