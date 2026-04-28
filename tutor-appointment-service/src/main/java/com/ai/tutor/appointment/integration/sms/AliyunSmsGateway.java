package com.ai.tutor.appointment.integration.sms;

public interface AliyunSmsGateway {

    SendResult sendVerifyCode(String phone, String outId);

    boolean checkVerifyCode(String phone, String outId, String code);

    record SendResult(String outId, String verifyCode) {
    }
}
