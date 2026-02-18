package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomStartReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;

public interface ChatRoomService {

    Long getOrCreateRoomWithUser(Long targetUid, Long uid);

    Long startChat(ChatRoomStartReq request, Long uid);

    CursorPageResp<ChatRoomItemResp> listRooms(ChatRoomPageReq request, Long uid);
}
