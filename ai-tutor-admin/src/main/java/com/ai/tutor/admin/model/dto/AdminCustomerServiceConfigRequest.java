package com.ai.tutor.admin.model.dto;

import lombok.Data;

@Data
public class AdminCustomerServiceConfigRequest {
    private Boolean enabled;
    private String channelType;
    private String displayName;
    private String wechatNo;
    private String qqNo;
    private String qrCodeObjectKey;
    private String serviceTime;
    private String description;
}
