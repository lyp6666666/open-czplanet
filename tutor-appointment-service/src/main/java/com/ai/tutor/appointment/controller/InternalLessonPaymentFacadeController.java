package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.service.LessonPaymentOrderService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.LessonPaymentPayInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/facade/lesson-payments")
@RequiredArgsConstructor
public class InternalLessonPaymentFacadeController {

    private final LessonPaymentOrderService lessonPaymentOrderService;

    @GetMapping("/{orderId}/payable")
    public BaseResponse<LessonPaymentPayInfo> getPayableOrder(@PathVariable("orderId") Long orderId,
                                                               @RequestParam("uid") Long uid) {
        return ResultUtils.success(lessonPaymentOrderService.getPayableOrder(orderId, uid));
    }

    @PostMapping("/payment-success")
    public BaseResponse<Boolean> onPaymentSuccess(@RequestBody PaymentSuccessEvent event) {
        ThrowUtils.throwIf(event == null, ErrorCode.PARAMS_ERROR);
        String ctx = event.getContextType() == null ? "" : event.getContextType().trim().toUpperCase();
        if (!"LESSON_PAYMENT_ORDER".equals(ctx)) {
            return ResultUtils.success(false);
        }
        lessonPaymentOrderService.handlePaymentSuccess(event);
        return ResultUtils.success(true);
    }
}
