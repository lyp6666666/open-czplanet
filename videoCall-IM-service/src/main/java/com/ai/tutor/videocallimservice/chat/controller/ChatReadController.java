package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatReadAckReq;
import com.ai.tutor.videocallimservice.chat.service.ChatReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/read")
@Tag(name = "已读接口", description = "提供已读上报能力")
public class ChatReadController {

    @Resource
    private ChatReadService chatReadService;

    @PostMapping("/ack")
    @Operation(summary = "已读上报")
    public BaseResponse<Boolean> ack(@Valid @RequestBody ChatReadAckReq request) {
        chatReadService.ackRead(request, RequestHolder.get().getUid());
        return ResultUtils.success(true);
    }
}
