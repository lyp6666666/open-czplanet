package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatTypingReq;
import com.ai.tutor.videocallimservice.chat.service.ChatTypingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/typing")
@Tag(name = "输入中接口", description = "提供会话输入中状态上报能力")
public class ChatTypingController {

    @Resource
    private ChatTypingService chatTypingService;

    @PostMapping
    @Operation(summary = "上报输入中状态")
    public BaseResponse<Boolean> report(@Valid @RequestBody ChatTypingReq request) {
        chatTypingService.reportTyping(request, RequestHolder.get().getUid());
        return ResultUtils.success(Boolean.TRUE);
    }
}
