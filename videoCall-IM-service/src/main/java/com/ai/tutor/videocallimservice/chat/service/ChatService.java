package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;

public interface ChatService {



    Long sendMsg(ChatMessageReq request, Long uid);

    ChatMessageResp getMsgResp(Long msgId, Long uid);
}
