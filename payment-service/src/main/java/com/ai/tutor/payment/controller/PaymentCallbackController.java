package com.ai.tutor.payment.controller;

import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.service.PaymentOrderService;
import com.alipay.api.internal.util.AlipaySignature;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器
 */
@Slf4j
@RestController
@RequestMapping("/payment/notify")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentProperties paymentProperties;

    /**
     * 支付宝异步回调
     */
    @PostMapping("/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        PaymentProperties.Alipay config = paymentProperties.getAlipay();
        try {
            // 调用SDK验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(params, config.getAlipayPublicKey(), "UTF-8", "RSA2");

            if (signVerified) {
                // 验签通过
                String outTradeNo = params.get("out_trade_no");
                String tradeStatus = params.get("trade_status");
                String tradeNo = params.get("trade_no");

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 注意：需要判断金额是否一致，这里简化处理
                    boolean updated = paymentOrderService.updateSuccess(outTradeNo, tradeNo, LocalDateTime.now());
                    if (updated) {
                        return "success";
                    } else {
                        log.warn("Alipay notify: Order status update failed or already updated. outTradeNo={}", outTradeNo);
                        return "success"; // 即使更新失败（如重复通知），也返回success给支付宝，避免重复推送
                    }
                }
                return "success";
            } else {
                log.error("Alipay signature verification failed");
                return "failure";
            }
        } catch (Exception e) {
            log.error("Alipay notify processing failed", e);
            return "failure";
        }
    }

    /**
     * 微信支付异步回调
     */
    @PostMapping("/wechat")
    public Map<String, String> wechatNotify(HttpServletRequest request, @RequestBody(required = false) String requestBody) {
        if (requestBody == null) {
             return Map.of("code", "FAIL", "message", "Request body is empty");
        }
        
        // 构建RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(request.getHeader("Wechatpay-Serial"))
                .nonce(request.getHeader("Wechatpay-Nonce"))
                .signature(request.getHeader("Wechatpay-Signature"))
                .timestamp(request.getHeader("Wechatpay-Timestamp"))
                .body(requestBody)
                .build();

        PaymentProperties.Wechat config = paymentProperties.getWechat();
        
        try {
            // 初始化NotificationConfig
            NotificationConfig notificationConfig = new RSAAutoCertificateConfig.Builder()
                    .merchantId(config.getMchId())
                    .privateKeyFromPath(config.getPrivateKeyPath())
                    .merchantSerialNumber(config.getCertificateSerialNo())
                    .apiV3Key(config.getApiV3Key())
                    .build();

            // 初始化NotificationParser
            NotificationParser parser = new NotificationParser(notificationConfig);
            
            // 验签并解密报文
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            
            log.info("Wechat pay notify received. outTradeNo: {}, transactionId: {}, tradeState: {}", 
                    transaction.getOutTradeNo(), transaction.getTransactionId(), transaction.getTradeState());

            if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
                // 更新订单状态
                // 微信返回的时间格式是 RFC3339，例如 2018-06-08T10:34:56+08:00
                // LocalDateTime.parse(transaction.getSuccessTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                LocalDateTime successTime = LocalDateTime.now();
                if (transaction.getSuccessTime() != null) {
                    try {
                        successTime = LocalDateTime.parse(transaction.getSuccessTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    } catch (Exception e) {
                        log.warn("Parse wechat success time failed: {}", transaction.getSuccessTime());
                    }
                }
                
                paymentOrderService.updateSuccess(transaction.getOutTradeNo(), transaction.getTransactionId(), successTime);
            } else {
                paymentOrderService.updateFailed(transaction.getOutTradeNo(), "TradeState: " + transaction.getTradeState());
            }

            return Map.of("code", "SUCCESS", "message", "成功");
        } catch (Exception e) {
            log.error("Wechat notify verification failed", e);
            // 验签失败或解析失败，返回 4xx/5xx 或者 FAIL
            // 微信支付期望返回 JSON
            return Map.of("code", "FAIL", "message", "失败: " + e.getMessage());
        }
    }
}
