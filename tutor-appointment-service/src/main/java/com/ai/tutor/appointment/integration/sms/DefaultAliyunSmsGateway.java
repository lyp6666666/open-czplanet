package com.ai.tutor.appointment.integration.sms;

import com.ai.tutor.appointment.config.SmsAliyunProperties;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAliyunSmsGateway implements AliyunSmsGateway {

    private final SmsAliyunProperties properties;

    @Override
    public SendResult sendVerifyCode(String phone, String outId) {
        validateRequiredConfig();
        try {
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setOutId(outId)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(properties.getTemplateParam())
                    .setCountryCode(properties.getCountryCode())
                    .setCodeLength(properties.getCodeLength())
                    .setCodeType(1L)
                    .setValidTime(properties.getValidTimeSeconds())
                    .setInterval(properties.getIntervalSeconds())
                    .setDuplicatePolicy(properties.getDuplicatePolicy())
                    .setReturnVerifyCode(properties.isReturnVerifyCode());
            if (StringUtils.hasText(properties.getSchemeName())) {
                request.setSchemeName(properties.getSchemeName());
            }
            SendSmsVerifyCodeResponse response = client().sendSmsVerifyCode(request);
            SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            SendSmsVerifyCodeResponseBody.SendSmsVerifyCodeResponseBodyModel model = body.getModel();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess())) {
                String requestId = model == null ? null : model.getRequestId();
                log.warn("Aliyun sms send rejected, phone: {}, outId: {}, code: {}, message: {}, requestId: {}, bizId: {}, accessDeniedDetail: {}",
                        phone, outId, body == null ? null : body.getCode(), body == null ? null : body.getMessage(),
                        requestId, model == null ? null : model.getBizId(),
                        body == null ? null : body.getAccessDeniedDetail());
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR,
                        "短信发送失败");
            }
            String returnedOutId = model == null || !StringUtils.hasText(model.getOutId()) ? outId : model.getOutId();
            return new SendResult(returnedOutId, model == null ? null : model.getVerifyCode());
        } catch (Exception e) {
            log.warn("Aliyun sms send failed, phone: {}, outId: {}, errorType: {}, message: {}", phone, outId,
                    e.getClass().getSimpleName(), e.getMessage(), e);
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "短信发送失败");
            return null;
        }
    }

    @Override
    public boolean checkVerifyCode(String phone, String outId, String code) {
        validateRequiredConfig();
        try {
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setOutId(outId)
                    .setVerifyCode(code)
                    .setCountryCode(properties.getCountryCode());
            if (StringUtils.hasText(properties.getSchemeName())) {
                request.setSchemeName(properties.getSchemeName());
            }
            CheckSmsVerifyCodeResponse response = client().checkSmsVerifyCode(request);
            CheckSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel model = body == null ? null : body.getModel();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess())) {
                log.warn("Aliyun sms verify rejected, phone: {}, outId: {}, code: {}, message: {}, verifyResult: {}, accessDeniedDetail: {}",
                        phone, outId, body == null ? null : body.getCode(), body == null ? null : body.getMessage(),
                        model == null ? null : model.getVerifyResult(),
                        body == null ? null : body.getAccessDeniedDetail());
            }
            return body != null
                    && Boolean.TRUE.equals(body.getSuccess())
                    && model != null
                    && "PASS".equalsIgnoreCase(model.getVerifyResult());
        } catch (Exception e) {
            log.warn("Aliyun sms verify failed, phone: {}, outId: {}, errorType: {}, message: {}", phone, outId,
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }

    private Client client() throws Exception {
        Config config = new Config()
                .setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret())
                .setEndpoint(properties.getEndpoint())
                .setRegionId(properties.getRegionId());
        return new Client(config);
    }

    private void validateRequiredConfig() {
        ThrowUtils.throwIf(!StringUtils.hasText(properties.getAccessKeyId()), ErrorCode.OPERATION_ERROR,
                "短信服务未配置：sms.aliyun.access-key-id");
        ThrowUtils.throwIf(!StringUtils.hasText(properties.getAccessKeySecret()), ErrorCode.OPERATION_ERROR,
                "短信服务未配置：sms.aliyun.access-key-secret");
        ThrowUtils.throwIf(!StringUtils.hasText(properties.getSignName()), ErrorCode.OPERATION_ERROR,
                "短信服务未配置：sms.aliyun.sign-name");
        ThrowUtils.throwIf(!StringUtils.hasText(properties.getTemplateCode()), ErrorCode.OPERATION_ERROR,
                "短信服务未配置：sms.aliyun.template-code");
    }
}
