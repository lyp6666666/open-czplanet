package com.ai.tutor.appointment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceConfigVO {
    private Boolean enabled;
    private String channelType;
    private String displayName;
    private String wechatNo;
    private String qqNo;
    private String qrCodeUrl;
    private String serviceTime;
    private String description;
}
