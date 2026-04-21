package com.ai.tutor.payment.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.payment.config.OpsProperties;
import com.ai.tutor.payment.controller.dto.PrepayRequest;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.controller.dto.PaymentOrderStatusResponse;
import com.ai.tutor.payment.service.YungouosPaymentAppService;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    private final OpsProperties opsProperties;

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

    @PostMapping("/dev/orders/{orderNo}/mock-success")
    @Operation(summary = "DEV/E2E 模拟支付成功", description = "仅供端到端测试模拟第三方支付成功，仍会触发真实业务结算完结逻辑")
    public BaseResponse<PaymentOrderStatusResponse> mockPaySuccess(@PathVariable("orderNo") String orderNo,
                                                                   @RequestHeader(value = "X-Ops-Token", required = false) String token) {
        String expected = opsProperties == null ? null : opsProperties.getVerifyToken();
        ThrowUtils.throwIf(expected == null || expected.isBlank(), ErrorCode.NO_AUTH_ERROR, "ops token not configured");
        ThrowUtils.throwIf(token == null || !token.equals(expected), ErrorCode.NO_AUTH_ERROR, "ops token invalid");
        return ResultUtils.success(yungouosPaymentAppService.mockPaySuccessForE2e(orderNo));
    }

    @PostMapping("/notify/yungouos")
    @Operation(summary = "YunGouOS 异步通知回调")
    public String notifyYungouos(HttpServletRequest request) {
        return yungouosPaymentAppService.handleNotify(request);
    }

    @GetMapping(value = "/return/yungouos", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "YunGouOS 同步回跳（return_url）")
    public String returnYungouos(HttpServletRequest request) {
        return yungouosPaymentAppService.handleReturn(request);
    }
}
