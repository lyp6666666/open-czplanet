package com.ai.tutor.payment.integration;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.payment.integration.feign.ImBrokerageOrderFeignClient;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ThrowUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "integration.brokerage.remote", name = "enabled", havingValue = "true")
public class HttpBrokerageOrderFacade implements BrokerageOrderFacade {

    private final ImBrokerageOrderFeignClient client;

    @Override
    public BrokerageOrderPayInfo getPayableOrder(Long brokerageOrderId, Long uid) {
        ThrowUtils.throwIf(brokerageOrderId == null || uid == null, ErrorCode.PARAMS_ERROR);
        requireCurrentUid(uid);

        BaseResponse<BrokerageOrderPayInfo> response = client.getPayableOrder(brokerageOrderId, uid);
        return unwrapData(response, "getPayableOrder");
    }

    private static void requireCurrentUid(Long uid) {
        RequestInfo info = RequestHolder.get();
        if (info == null || info.getUid() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        ThrowUtils.throwIf(!uid.equals(info.getUid()), ErrorCode.NO_AUTH_ERROR);
    }

    private static <T> T unwrapData(BaseResponse<T> response, String action) {
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Brokerage facade call failed: " + action + " response is null");
        }
        if (response.getCode() != ErrorCode.SUCCESS.getCode()) {
            String message = response.getMessage();
            if (message == null || message.isBlank()) {
                message = "Brokerage facade call failed: " + action;
            }
            throw new BusinessException(response.getCode(), message);
        }
        return response.getData();
    }
}
