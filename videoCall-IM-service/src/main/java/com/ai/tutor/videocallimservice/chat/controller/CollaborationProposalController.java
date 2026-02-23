package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.RespondCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.service.CollaborationProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/collaboration")
@Tag(name = "合作提案接口", description = "在聊天中发起与响应合作提案")
public class CollaborationProposalController {

    @Resource
    private CollaborationProposalService collaborationProposalService;
    @Resource
    private ChatService chatService;

    @PostMapping("/proposal")
    @Operation(summary = "发起合作提案")
    public BaseResponse<ChatMessageResp> create(@Valid @RequestBody CreateCollaborationProposalReq req) {
        Long uid = RequestHolder.get().getUid();
        Long msgId = collaborationProposalService.createAndSend(req, uid);
        return ResultUtils.success(chatService.getMsgResp(msgId, uid));
    }

    @PostMapping("/proposal/{proposalId}/response")
    @Operation(summary = "响应合作提案")
    public BaseResponse<ChatMessageResp> respond(@PathVariable("proposalId") Long proposalId,
                                                 @Valid @RequestBody RespondCollaborationProposalReq req) {
        Long uid = RequestHolder.get().getUid();
        Long msgId = collaborationProposalService.respondAndSend(proposalId, req, uid);
        return ResultUtils.success(chatService.getMsgResp(msgId, uid));
    }
}
