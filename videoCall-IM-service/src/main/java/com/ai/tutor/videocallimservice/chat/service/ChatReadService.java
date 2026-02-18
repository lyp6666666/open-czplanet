package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatReadAckReq;

public interface ChatReadService {
    void ackRead(ChatReadAckReq request, Long uid);
}
