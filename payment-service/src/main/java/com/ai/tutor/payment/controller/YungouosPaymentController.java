package com.ai.tutor.payment.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.controller.dto.PaymentOrderStatusResponse;
import com.ai.tutor.payment.service.YungouosPaymentAppService;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * YunGouOS 支付接口（收银台/轮询使用）
 */
@RestController
@RequestMapping("/payment")
@Tag(name = "支付接口", description = "YunGouOS 统一下单、查单与回调")
@RequiredArgsConstructor
public class YungouosPaymentController {

    private final YungouosPaymentAppService yungouosPaymentAppService;

    @PostMapping("/prepay")
    @Operation(summary = "统一下单（扫码支付出码）")
    public BaseResponse<PrepayResponse> prepay(@Valid @RequestBody PrepayRequest req) {
        Long uid = RequestHolder.get().getUid();
        String ip = RequestHolder.get().getIp();
        return ResultUtils.success(yungouosPaymentAppService.prepay(req, uid, ip));
    }

    @GetMapping("/orders/{orderNo}")
    @Operation(summary = "查询支付单状态（收银台轮询）")
    public BaseResponse<PaymentOrderStatusResponse> getOrderStatus(@PathVariable("orderNo") String orderNo) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(yungouosPaymentAppService.getOrderStatus(orderNo, uid));
    }

    @PostMapping("/notify/yungouos")
    @Operation(summary = "YunGouOS 异步通知回调")
    public String notifyYungouos(HttpServletRequest request) {
        return yungouosPaymentAppService.handleNotify(request);
    }
}

