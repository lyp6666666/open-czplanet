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
public class RoomReadState {
    private Long id;
    private Long roomId;
    private Long uid;
    private Long lastReadMsgId;
    private LocalDateTime lastReadTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
