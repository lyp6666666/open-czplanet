package com.ai.tutor.payment.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.utils.ResultUtils;
import com.yungouos.pay.util.PaySignUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/debug/payment")
@Profile({"qa", "test"})
@RequiredArgsConstructor
public class InternalPaymentDebugController {

    private final PaymentProperties paymentProperties;

    @PostMapping("/yungouos-sign")
    public BaseResponse<String> yungouosSign(@RequestBody Map<String, Object> params) {
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        boolean mock = config != null && StringUtils.hasText(config.getBaseUrl()) && config.getBaseUrl().startsWith("mock://");
        String appKey = config == null ? null : config.getAppKey();
        if (mock && !StringUtils.hasText(appKey)) {
            appKey = "TEST_KEY";
        }
        if (!StringUtils.hasText(appKey)) {
            return new BaseResponse<>(50000, null, "missing appKey");
        }
        Map<String, Object> signParams = new HashMap<>();
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (e.getKey() == null) continue;
                if ("sign".equalsIgnoreCase(e.getKey())) continue;
                Object v = e.getValue();
                if (v == null) continue;
                String s = String.valueOf(v);
                if (!StringUtils.hasText(s)) continue;
                signParams.put(e.getKey(), s);
            }
        }
        return ResultUtils.success(PaySignUtil.createSign(signParams, appKey));
    }
}
