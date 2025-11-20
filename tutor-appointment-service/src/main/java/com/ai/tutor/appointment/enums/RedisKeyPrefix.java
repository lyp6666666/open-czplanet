package com.ai.tutor.appointment.enums;

public enum RedisKeyPrefix {

    SMS_CODE("sms:code:", "短信验证码"),
    USER_TOKEN("user:token:", "用户JWT令牌"),
    USER_INFO("user:info:", "用户基本信息缓存");

    private final String prefix;
    private final String description;

    RedisKeyPrefix(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }

    /**
     * 拼接完整Key
     */
    public String key(String suffix) {
        return prefix + suffix;
    }
}
