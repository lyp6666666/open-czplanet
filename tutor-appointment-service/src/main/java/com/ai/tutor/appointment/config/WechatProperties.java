package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatProperties {
    private String appid;
    private String secret;
    private String token;
    private String aesKey;
    private String msgDataFormat;

    /**
     * 是否启用 Mock 模式（用于开发环境跳过真实微信验证）
     */
    private Boolean mockEnabled = false;
}
