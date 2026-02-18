package com.ai.tutor.integration;

import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * IM 门面在单体阶段的本地实现。
 *
 * <p>该实现位于 starter 模块，避免预约模块直接依赖 IM 模块内部实现。</p>
 */
@Component
public class LocalImFacade implements ImFacade {

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatService chatService;

    @Override
    public Long getOrCreateRoomWithUser(Long uid, Long targetUid) {
        return chatRoomService.getOrCreateRoomWithUser(targetUid, uid);
    }

    @Override
    public Long sendSystemMessage(Long uid, Long roomId, Object body) {
        ChatMessageReq req = ChatMessageReq.builder()
                .roomId(roomId)
                .msgType(8)
                .body(body)
                .build();
        return chatService.sendMsg(req, uid);
    }

    @Override
    public List<Long> listRecentContactUids(Long uid, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        ChatRoomPageReq req = new ChatRoomPageReq();
        req.setPageSize(safeLimit);
        req.setCursor(null);
        CursorPageResp<ChatRoomItemResp> page = chatRoomService.listRooms(req, uid);
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        for (ChatRoomItemResp it : page.getList()) {
            if (it == null || it.getOtherUid() == null) {
                continue;
            }
            ordered.add(it.getOtherUid());
            if (ordered.size() >= safeLimit) {
                break;
            }
        }
        return new ArrayList<>(ordered);
    }
}
