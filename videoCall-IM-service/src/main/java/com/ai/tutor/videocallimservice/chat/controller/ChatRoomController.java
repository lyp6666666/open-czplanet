package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomCreateReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/room")
@Tag(name = "会话接口", description = "提供会话创建与会话列表接口")
public class ChatRoomController {

    @Resource
    private ChatRoomService chatRoomService;

    @PostMapping
    @Operation(summary = "创建或获取会话", description = "根据对方用户id获取1对1会话，不存在则创建")
    public BaseResponse<Long> getOrCreate(@Valid @RequestBody ChatRoomCreateReq request) {
        Long roomId = chatRoomService.getOrCreateRoomWithUser(request.getTargetUid(), RequestHolder.get().getUid());
        return ResultUtils.success(roomId);
    }

    @GetMapping("/page")
    @Operation(summary = "会话列表（游标分页）")
    public BaseResponse<CursorPageResp<ChatRoomItemResp>> page(@Valid ChatRoomPageReq request) {
        return ResultUtils.success(chatRoomService.listRooms(request, RequestHolder.get().getUid()));
    }
}

