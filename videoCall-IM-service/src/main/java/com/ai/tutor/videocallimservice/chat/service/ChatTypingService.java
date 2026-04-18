package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatTypingReq;

public interface ChatTypingService {
    void reportTyping(ChatTypingReq request, Long uid);
}
