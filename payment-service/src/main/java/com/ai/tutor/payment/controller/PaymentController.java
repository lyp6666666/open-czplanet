package com.ai.tutor.payment.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.service.PaymentAppService;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 原生支付接口 (WeChat/Alipay)
 */
@RestController
@RequestMapping("/payment")
@Tag(name = "原生支付接口", description = "微信/支付宝统一下单")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentAppService paymentAppService;

    @PostMapping("/create")
    @Operation(summary = "创建支付订单 (Native/JSAPI)")
    public BaseResponse<PrepayResponse> create(@Valid @RequestBody PrepayRequest req) {
        Long uid = RequestHolder.get().getUid();
        String ip = RequestHolder.get().getIp();
        return ResultUtils.success(paymentAppService.prepay(req, uid, ip));
    }
}
