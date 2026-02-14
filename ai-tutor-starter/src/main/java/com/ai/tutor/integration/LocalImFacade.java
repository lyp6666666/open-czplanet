package com.ai.tutor.integration;

import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * IM 门面在单体阶段的本地实现。
 *
 * <p>该实现位于 starter 模块，避免预约模块直接依赖 IM 模块内部实现。</p>
 */
@Component
public class LocalImFacade implements ImFacade {

    @Resource
    private ChatRoomService chatRoomService;

    @Override
    public Long getOrCreateRoomWithUser(Long uid, Long targetUid) {
        return chatRoomService.getOrCreateRoomWithUser(targetUid, uid);
    }
}

