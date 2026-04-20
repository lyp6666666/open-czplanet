package com.ai.tutor.payment.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.LessonPaymentPayInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "tutor-appointment-service")
public interface AppointmentLessonPaymentFeignClient {

    @GetMapping("/internal/facade/lesson-payments/{orderId}/payable")
    BaseResponse<LessonPaymentPayInfo> getPayableOrder(@PathVariable("orderId") Long orderId,
                                                        @RequestParam("uid") Long uid);

    @PostMapping("/internal/facade/lesson-payments/payment-success")
    BaseResponse<Boolean> onPaymentSuccess(@RequestBody PaymentSuccessEvent event);
}
