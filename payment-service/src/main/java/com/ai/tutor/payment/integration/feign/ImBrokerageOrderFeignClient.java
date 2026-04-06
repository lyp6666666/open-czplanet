package com.ai.tutor.payment.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "videoCall-IM-service")
public interface ImBrokerageOrderFeignClient {

    @GetMapping("/internal/facade/brokerage/orders/{orderId}/payable")
    BaseResponse<BrokerageOrderPayInfo> getPayableOrder(@PathVariable("orderId") Long orderId,
                                                         @RequestParam("uid") Long uid);

    @PostMapping("/internal/facade/payment/success")
    BaseResponse<Boolean> onPaymentSuccess(@RequestHeader("X-Uid") String uid,
                                           @RequestHeader("X-Role") String role,
                                           @RequestHeader("X-Ts") String ts,
                                           @RequestHeader("X-Auth-Sign") String sign,
                                           @RequestBody PaymentSuccessEvent event);
}
