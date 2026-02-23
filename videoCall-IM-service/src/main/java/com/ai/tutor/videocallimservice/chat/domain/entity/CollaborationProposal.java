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
public class CollaborationProposal {
    private Long id;
    private Long roomId;
    private Long fromUid;
    private Long toUid;
    private String pricePerHour;
    private String classTime;
    private Integer frequencyPerWeek;
    private String status;
    private Long actorUid;
    private LocalDateTime actionTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
