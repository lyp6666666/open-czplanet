package com.ai.tutor.appointment.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeCarouselConfig {
    private Long id;
    private String title;
    private String subtitle;
    private String imageObjectKey;
    private String linkType;
    private String linkUrl;
    private Integer sortOrder;
    private Long createAdminId;
    private Long updateAdminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
