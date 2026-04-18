package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatPresenceResp;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/presence")
@Tag(name = "聊天在线状态", description = "提供会话用户在线状态查询")
public class ChatPresenceController {

    @Resource
    private SseSessionManager sseSessionManager;

    @GetMapping("/batch")
    @Operation(summary = "批量查询在线状态")
    public BaseResponse<List<ChatPresenceResp>> batch(@RequestParam("uids") List<Long> uids) {
        return ResultUtils.success(sseSessionManager.listPresence(uids));
    }
}
