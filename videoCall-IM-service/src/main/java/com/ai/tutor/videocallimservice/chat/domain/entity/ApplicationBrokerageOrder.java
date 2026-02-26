package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationBrokerageOrder {
    private Long id;
    private Long applicationId;
    private Long orderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
