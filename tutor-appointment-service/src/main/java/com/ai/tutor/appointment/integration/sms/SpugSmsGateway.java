package com.ai.tutor.appointment.integration.sms;

public interface SpugSmsGateway {

    void sendVerifyCode(String phone, String code);
}
