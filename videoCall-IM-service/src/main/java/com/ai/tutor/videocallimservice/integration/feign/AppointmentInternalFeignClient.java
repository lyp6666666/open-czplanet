package com.ai.tutor.videocallimservice.integration.feign;

import com.ai.tutor.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "tutor-appointment-service")
public interface AppointmentInternalFeignClient {

    @GetMapping("/internal/facade/users/{uid}/basic")
    BaseResponse<Map<String, Object>> getUserBasicById(@PathVariable("uid") Long uid);
}
