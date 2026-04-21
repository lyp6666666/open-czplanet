package com.ai.tutor.liveclass.integration.im;

import com.ai.tutor.common.BaseResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "videoCall-IM-service")
public interface ImInternalFeignClient {

    @PostMapping("/internal/facade/im/messages/system")
    BaseResponse<Long> sendSystemMessage(@RequestBody ImSystemMessageRequest request);

    @Data
    class ImSystemMessageRequest {
        private Long roomId;
        private Object body;
    }
}
