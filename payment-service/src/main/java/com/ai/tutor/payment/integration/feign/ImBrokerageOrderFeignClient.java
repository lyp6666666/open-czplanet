package com.ai.tutor.payment.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "videoCall-IM-service")
public interface ImBrokerageOrderFeignClient {

    @GetMapping("/internal/facade/brokerage/orders/{orderId}/payable")
    BaseResponse<BrokerageOrderPayInfo> getPayableOrder(@PathVariable("orderId") Long orderId,
                                                         @RequestParam("uid") Long uid);
}
