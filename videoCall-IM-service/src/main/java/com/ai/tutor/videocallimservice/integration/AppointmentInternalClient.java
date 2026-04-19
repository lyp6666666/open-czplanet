package com.ai.tutor.videocallimservice.integration;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.common.integration.InviteSystemBenefitInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.integration.feign.AppointmentInternalFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "integration.appointment.remote", name = "enabled", havingValue = "true")
public class AppointmentInternalClient {

    private final AppointmentInternalFeignClient client;

    public ImUser getUserBasicById(Long uid) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        BaseResponse<Map<String, Object>> response = client.getUserBasicById(uid);
        Map<String, Object> basicInfo = unwrapData(response, "getUserBasicById");
        return toImUser(basicInfo);
    }

    public String getUserPhoneById(Long uid) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        BaseResponse<String> response = client.getUserPhoneById(uid);
        String phone = unwrapData(response, "getUserPhoneById");
        return phone == null ? "" : phone;
    }

    public void notifyInviteBrokeragePaid(InviteBrokeragePaidEvent event) {
        ThrowUtils.throwIf(event == null || event.getBrokerageOrderId() == null, ErrorCode.PARAMS_ERROR);
        BaseResponse<Boolean> response = client.notifyInviteBrokeragePaid(event);
        unwrapData(response, "notifyInviteBrokeragePaid");
    }

    public InviteSystemBenefitInfo getInviteSystemBenefit(Long uid) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        BaseResponse<InviteSystemBenefitInfo> response = client.getInviteSystemBenefit(uid);
        return unwrapData(response, "getInviteSystemBenefit");
    }

    private static ImUser toImUser(Map<String, Object> info) {
        if (info == null) {
            return null;
        }
        ImUser user = new ImUser();
        user.setId(toLong(info.get("id")));
        user.setUserType(toInteger(info.get("userType")));
        user.setRefId(toLong(info.get("refId")));
        user.setStatus(toInteger(info.get("status")));
        return user;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            return number.longValue();
        }
        if (value instanceof String) {
            String text = (String) value;
            if (text.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: invalid long value");
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: invalid long value");
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: invalid long type");
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            return number.intValue();
        }
        if (value instanceof String) {
            String text = (String) value;
            if (text.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: invalid integer value");
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: invalid integer value");
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: invalid integer type");
    }

    private static <T> T unwrapData(BaseResponse<T> response, String action) {
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Appointment internal call failed: " + action + " response is null");
        }
        if (response.getCode() != ErrorCode.SUCCESS.getCode()) {
            String message = response.getMessage();
            if (message == null || message.isBlank()) {
                message = "Appointment internal call failed: " + action;
            }
            throw new BusinessException(response.getCode(), message);
        }
        return response.getData();
    }
}
