package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "customer-service")
public class CustomerServiceProperties {
    private Boolean enabled = true;
    private String channelType = "WECHAT_WORK";
    private String displayName = "创智星球客服";
    private String wechatNo = "ai_tutor_service";
    private String qqNo = "123456789";
    private String qrCodeObjectKey;
    private String serviceTime = "09:00 - 22:00";
    private String description = "添加客服时请备注：家长/老师 + 手机号";
}
