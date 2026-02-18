package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Data;

@Data
public class RoomUnreadCount {
    private Long roomId;
    private Long unreadCount;
}
