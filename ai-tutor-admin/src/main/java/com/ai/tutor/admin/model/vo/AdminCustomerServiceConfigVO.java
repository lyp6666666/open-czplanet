package com.ai.tutor.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerServiceConfigVO {
    private Boolean enabled;
    private String channelType;
    private String displayName;
    private String wechatNo;
    private String qqNo;
    private String qrCodeUrl;
    private String qrCodeObjectKey;
    private String serviceTime;
    private String description;
    private LocalDateTime updateTime;
}
