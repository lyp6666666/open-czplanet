package com.ai.tutor.admin.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.admin.model.dto.PaymentRefundRequest;
import com.ai.tutor.admin.model.dto.PaymentRefundResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentRefundFeignClient {

    @PostMapping("/payment/refund")
    BaseResponse<PaymentRefundResponse> refund(@RequestBody PaymentRefundRequest request);
}

