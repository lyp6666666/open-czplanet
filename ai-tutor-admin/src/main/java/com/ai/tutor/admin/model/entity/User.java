package com.ai.tutor.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String phone;
    private String avatar;
    private Integer sex;
    private String openId;
    private Integer activeStatus;
    private LocalDateTime lastOptTime;
    private String ipInfo;
    private Long itemId;
    private Integer status;
    private Integer userType;
    private Long refId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
