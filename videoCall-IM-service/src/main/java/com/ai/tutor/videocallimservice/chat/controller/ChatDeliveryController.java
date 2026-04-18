package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatDeliveryAckReq;
import com.ai.tutor.videocallimservice.chat.service.ChatDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/delivery")
@Tag(name = "送达回执接口", description = "提供消息送达确认能力")
public class ChatDeliveryController {

    @Resource
    private ChatDeliveryService chatDeliveryService;

    @PostMapping("/ack")
    @Operation(summary = "消息送达确认")
    public BaseResponse<Boolean> ack(@Valid @RequestBody ChatDeliveryAckReq request) {
        chatDeliveryService.ackDelivered(request, RequestHolder.get().getUid());
        return ResultUtils.success(Boolean.TRUE);
    }
}
