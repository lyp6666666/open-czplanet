package com.ai.tutor.appointment.service;

public interface SmsService {

    /**
     * 生成并发送验证码
     * @param phone
     * @return
     */
    String sendCode(String phone,String prefix);


    /**
     * 验证验证码
     * @param phone
     * @param code
     * @return
     */
    boolean verifyCode(String phone, String code,String prefix);
}
