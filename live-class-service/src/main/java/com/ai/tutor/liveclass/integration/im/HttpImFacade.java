package com.ai.tutor.liveclass.integration.im;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ThrowUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HttpImFacade {

    private final ImInternalFeignClient client;

    public Long sendSystemMessage(Long uid, Long roomId, Object body) {
        ThrowUtils.throwIf(uid == null || roomId == null || body == null, ErrorCode.PARAMS_ERROR);
        RequestInfo info = RequestHolder.get();
        if (info == null || info.getUid() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        ThrowUtils.throwIf(!uid.equals(info.getUid()), ErrorCode.NO_AUTH_ERROR);

        ImInternalFeignClient.ImSystemMessageRequest request = new ImInternalFeignClient.ImSystemMessageRequest();
        request.setRoomId(roomId);
        request.setBody(body);
        BaseResponse<Long> response = client.sendSystemMessage(request);
        return unwrap(response, "sendSystemMessage");
    }

    private static <T> T unwrap(BaseResponse<T> response, String action) {
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "IM facade call failed: " + action + " response is null");
        }
        if (response.getCode() != ErrorCode.SUCCESS.getCode()) {
            String message = response.getMessage();
            if (message == null || message.isBlank()) {
                message = "IM facade call failed: " + action;
            }
            throw new BusinessException(response.getCode(), message);
        }
        return response.getData();
    }
}
