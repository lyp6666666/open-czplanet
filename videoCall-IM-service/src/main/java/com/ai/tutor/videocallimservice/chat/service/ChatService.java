package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessagePageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageSearchReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;

public interface ChatService {
    CursorPageBaseResp<ChatMessageResp> getMsgPage(ChatMessagePageReq request, Long uid);

    CursorPageBaseResp<ChatMessageResp> searchMsgPage(ChatMessageSearchReq request, Long uid);

    Long sendMsg(ChatMessageReq request, Long uid);

    ChatMessageResp getMsgResp(Long msgId, Long uid);
}
