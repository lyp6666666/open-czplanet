package com.ai.tutor.appointment.integration.feign;

import com.ai.tutor.common.BaseResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@FeignClient(name = "live-class-service")
public interface LiveClassInternalFeignClient {

    @PostMapping("/internal/live/sessions/sync-from-course")
    BaseResponse<LiveSessionResp> syncFromCourse(@RequestBody SyncCourseSessionRequest request);

    @Data
    class SyncCourseSessionRequest {
        private Long courseId;
        private Long scheduleEventId;
        private Long roomId;
        private Long teacherUid;
        private Long studentUid;
        private String title;
        private LocalDateTime scheduledStartAt;
        private LocalDateTime scheduledEndAt;
        private String recordPolicy;
        private String aiPolicy;
    }

    @Data
    class LiveSessionResp {
        private Long sessionId;
        private Long courseId;
        private String status;
    }
}
