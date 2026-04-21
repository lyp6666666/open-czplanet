package com.ai.tutor.videocallimservice.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.common.integration.InviteSystemBenefitInfo;
import com.ai.tutor.videocallimservice.integration.dto.InternalUserEmailsVO;
import lombok.Data;
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

    @GetMapping("/internal/facade/users/{uid}/emails")
    BaseResponse<InternalUserEmailsVO> getUserEmailsById(@PathVariable("uid") Long uid);

    @GetMapping("/internal/facade/users/{uid}/schedule/conflict-check")
    BaseResponse<Boolean> checkScheduleConflict(@PathVariable("uid") Long uid,
                                                @org.springframework.web.bind.annotation.RequestParam("otherUid") Long otherUid,
                                                @org.springframework.web.bind.annotation.RequestParam("startAt") Long startAt,
                                                @org.springframework.web.bind.annotation.RequestParam("endAt") Long endAt);

    @PostMapping("/internal/facade/schedule/trial-events")
    BaseResponse<Long> createAcceptedTrialEvent(@RequestBody InternalTrialEventRequest request);

    @PostMapping("/internal/facade/invite/brokerage-paid")
    BaseResponse<Boolean> notifyInviteBrokeragePaid(@RequestBody InviteBrokeragePaidEvent event);

    @GetMapping("/internal/facade/invite/system-benefit/{uid}")
    BaseResponse<InviteSystemBenefitInfo> getInviteSystemBenefit(@PathVariable("uid") Long uid);

    @Data
    class InternalTrialEventRequest {
        private Long courseId;
        private Long roomId;
        private Long teacherUid;
        private Long studentUid;
        private Long createdBy;
        private String title;
        private String lessonPrice;
        private Long startAt;
        private Long endAt;
        private String remark;
        private String clientRequestId;
    }
}
