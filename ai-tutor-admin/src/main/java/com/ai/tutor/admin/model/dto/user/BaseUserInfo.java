package com.ai.tutor.admin.model.dto.user;

import lombok.Data;

@Data
public class BaseUserInfo {
    private String name;
    private String avatar;
    private Integer sex;
    private Integer activeStatus;
    private Long itemId;
    private Integer status;
    private String ipInfo;
}
