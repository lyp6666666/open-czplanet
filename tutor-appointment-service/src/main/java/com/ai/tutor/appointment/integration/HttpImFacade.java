package com.ai.tutor.appointment.integration;

import com.ai.tutor.appointment.integration.feign.ImInternalFeignClient;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ThrowUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "integration.im.remote", name = "enabled", havingValue = "true")
public class HttpImFacade implements ImFacade {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ImInternalFeignClient client;

    @Override
    public Long getOrCreateRoomWithUser(Long uid, Long targetUid) {
        ThrowUtils.throwIf(targetUid == null, ErrorCode.PARAMS_ERROR);
        requireCurrentUid(uid);

        ImInternalFeignClient.ImRoomRequest request = new ImInternalFeignClient.ImRoomRequest();
        request.setTargetUid(targetUid);
        BaseResponse<Long> response = client.getOrCreateRoomWithUser(request);
        return unwrapData(response, "getOrCreateRoomWithUser");
    }

    @Override
    public Long sendSystemMessage(Long uid, Long roomId, Object body) {
        ThrowUtils.throwIf(roomId == null || body == null, ErrorCode.PARAMS_ERROR);
        requireCurrentUid(uid);

        ImInternalFeignClient.ImSystemMessageRequest request = new ImInternalFeignClient.ImSystemMessageRequest();
        request.setRoomId(roomId);
        request.setBody(body);
        BaseResponse<Long> response = client.sendSystemMessage(request);
        return unwrapData(response, "sendSystemMessage");
    }

    @Override
    public List<Long> listRecentContactUids(Long uid, int limit) {
        requireCurrentUid(uid);
        int safeLimit = normalizeLimit(limit);
        BaseResponse<List<Long>> response = client.listRecentContactUids(safeLimit);
        List<Long> data = unwrapData(response, "listRecentContactUids");
        return data == null ? List.of() : data;
    }

    private static void requireCurrentUid(Long uid) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        RequestInfo info = RequestHolder.get();
        if (info == null || info.getUid() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        ThrowUtils.throwIf(!uid.equals(info.getUid()), ErrorCode.NO_AUTH_ERROR);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static <T> T unwrapData(BaseResponse<T> response, String action) {
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
