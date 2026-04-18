package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatDeliveryAckReq;

public interface ChatDeliveryService {
    void ackDelivered(ChatDeliveryAckReq request, Long uid);
}
