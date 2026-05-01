package com.ai.tutor.admin.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerServiceConfig {
    private Long id;
    private Boolean enabled;
    private String channelType;
    private String displayName;
    private String wechatNo;
    private String qqNo;
    private String qrCodeObjectKey;
    private String serviceTime;
    private String description;
    private Long updateAdminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
