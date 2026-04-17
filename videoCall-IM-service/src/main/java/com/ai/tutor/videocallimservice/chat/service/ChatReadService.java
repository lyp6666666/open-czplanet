package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatReadAckReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatReadAckResp;

public interface ChatReadService {
    ChatReadAckResp ackRead(ChatReadAckReq request, Long uid);
}
