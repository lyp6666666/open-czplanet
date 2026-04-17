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
public class AdminHomeCarouselItemVO {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String objectKey;
    private String linkType;
    private String linkUrl;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
