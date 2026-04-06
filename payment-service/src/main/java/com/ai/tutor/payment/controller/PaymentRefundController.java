package com.ai.tutor.payment.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.payment.controller.dto.InternalRefundRequest;
import com.ai.tutor.payment.controller.dto.InternalRefundResponse;
import com.ai.tutor.payment.service.PaymentRefundAppService;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 退款接口（管理端审核通过后调用）
 *
 * <p>该接口需要通过网关签名身份校验（X-Uid/X-Role/X-Ts/X-Auth-Sign）。</p>
 */
@RestController
@RequestMapping("/payment")
@Tag(name = "退款接口", description = "管理端审核通过后发起原路退款（内部调用）")
@RequiredArgsConstructor
public class PaymentRefundController {

    private final PaymentRefundAppService paymentRefundAppService;

    @PostMapping("/refund")
    @Operation(summary = "发起原路退款（幂等）")
    public BaseResponse<InternalRefundResponse> refund(@Valid @RequestBody InternalRefundRequest request) {
        return ResultUtils.success(paymentRefundAppService.refund(request));
    }
}

