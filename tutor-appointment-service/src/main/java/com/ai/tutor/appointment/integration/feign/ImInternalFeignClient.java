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

    @GetMapping("/internal/facade/im/contacts/recent")
    BaseResponse<List<Long>> listRecentContactUids(@RequestParam("limit") Integer limit);

    @Data
    class ImRoomRequest {
        private Long targetUid;
    }

    @Data
    class ImSystemMessageRequest {
        private Long roomId;
        private Object body;
    }
}
