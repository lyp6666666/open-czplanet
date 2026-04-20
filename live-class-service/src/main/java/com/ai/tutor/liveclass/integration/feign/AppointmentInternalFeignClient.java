package com.ai.tutor.liveclass.integration.feign;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.LessonPaymentAccessCheckInfo;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "tutor-appointment-service")
public interface AppointmentInternalFeignClient {

    @GetMapping("/user/batch")
    BaseResponse<List<UserSimpleDto>> batchUsers(@RequestParam("ids") String ids);

    @GetMapping("/internal/facade/lesson-payments/lesson/{lessonId}/join-access")
    BaseResponse<LessonPaymentAccessCheckInfo> getLessonJoinAccess(@PathVariable("lessonId") Long lessonId);

    @Data
    class UserSimpleDto {
        private Long id;
        private String name;
        private String realName;
        private String avatar;
        private Integer userType;
    }
}
