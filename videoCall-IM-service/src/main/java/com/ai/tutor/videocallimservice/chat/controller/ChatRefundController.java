package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ApplyChatRefundReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RefundStateResp;
import com.ai.tutor.videocallimservice.chat.service.ChatRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天阶段退款接口
 */
@RestController
@RequestMapping("/chat/refund")
@Tag(name = "聊天退款接口", description = "聊天阶段申请退费与按钮状态查询")
public class ChatRefundController {

    @Resource
    private ChatRefundService chatRefundService;

    @GetMapping("/state")
    @Operation(summary = "查询聊天页退款按钮状态")
    public BaseResponse<RefundStateResp> getRefundState(@RequestParam("roomId") Long roomId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(chatRefundService.getRefundState(roomId, uid));
    }

    @PostMapping("/apply")
    @Operation(summary = "申请聊天退费（申请后立即关闭聊天）")
    public BaseResponse<ChatMessageResp> applyChatRefund(@Valid @RequestBody ApplyChatRefundReq request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(chatRefundService.applyChatRefund(request.getRoomId(), request.getReason(), uid));
    }
}
