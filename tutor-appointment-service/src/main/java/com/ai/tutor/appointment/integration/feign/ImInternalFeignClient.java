package com.ai.tutor.appointment.integration.feign;

import com.ai.tutor.common.BaseResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "videoCall-IM-service")
public interface ImInternalFeignClient {

    @PostMapping("/internal/facade/im/rooms/with-user")
    BaseResponse<Long> getOrCreateRoomWithUser(@RequestBody ImRoomRequest request);

    @PostMapping("/internal/facade/im/messages/system")
    BaseResponse<Long> sendSystemMessage(@RequestBody ImSystemMessageRequest request);

    @PostMapping("/internal/facade/im/rooms/{roomId}/schedule-ready")
    BaseResponse<Boolean> assertRoomReadyForScheduling(@org.springframework.web.bind.annotation.PathVariable("roomId") Long roomId);

    @GetMapping("/internal/facade/im/contacts/recent")
    BaseResponse<List<Long>> listRecentContactUids(@RequestParam("limit") Integer limit);

    @PostMapping("/internal/facade/courses/{courseId}/weekly-schedule-submitted")
    BaseResponse<Boolean> confirmWeeklyScheduleSubmitted(@org.springframework.web.bind.annotation.PathVariable("courseId") Long courseId,
                                                         @RequestBody ConfirmWeeklyScheduleRequest request);

    @PostMapping("/internal/facade/courses/{courseId}/trial-canceled")
    BaseResponse<Boolean> markTrialCanceled(@org.springframework.web.bind.annotation.PathVariable("courseId") Long courseId,
                                            @RequestBody TrialCanceledRequest request);

    @Data
    class ImRoomRequest {
        private Long targetUid;
    }

    @Data
    class ImSystemMessageRequest {
        private Long roomId;
        private Object body;
    }

    @Data
    class ConfirmWeeklyScheduleRequest {
        private String classTime;
        private Integer frequencyPerWeek;
        private Long lessonPriceFen;
    }

    @Data
    class TrialCanceledRequest {
        private String reason;
    }
}
