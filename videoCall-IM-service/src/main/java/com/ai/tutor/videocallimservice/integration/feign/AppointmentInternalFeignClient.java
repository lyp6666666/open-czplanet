package com.ai.tutor.videocallimservice.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.common.integration.InviteSystemBenefitInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "tutor-appointment-service")
public interface AppointmentInternalFeignClient {

    @GetMapping("/internal/facade/users/{uid}/basic")
    BaseResponse<Map<String, Object>> getUserBasicById(@PathVariable("uid") Long uid);

    @GetMapping("/internal/facade/users/{uid}/phone")
    BaseResponse<String> getUserPhoneById(@PathVariable("uid") Long uid);

    @PostMapping("/internal/facade/invite/brokerage-paid")
    BaseResponse<Boolean> notifyInviteBrokeragePaid(@RequestBody InviteBrokeragePaidEvent event);

    @GetMapping("/internal/facade/invite/system-benefit/{uid}")
    BaseResponse<InviteSystemBenefitInfo> getInviteSystemBenefit(@PathVariable("uid") Long uid);
}
