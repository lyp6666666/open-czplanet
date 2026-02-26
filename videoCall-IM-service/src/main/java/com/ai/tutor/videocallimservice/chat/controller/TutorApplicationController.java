package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateTutorApplicationReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.DecideTutorApplicationReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.TutorApplicationPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationEnterResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationUnreadResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationVO;
import com.ai.tutor.videocallimservice.chat.service.TutorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/application")
@Tag(name = "申请接口", description = "找家教申请创建、处理与聊天准入")
public class TutorApplicationController {

    @Resource
    private TutorApplicationService tutorApplicationService;

    @PostMapping
    @Operation(summary = "创建申请")
    public BaseResponse<TutorApplicationVO> create(@Valid @RequestBody CreateTutorApplicationReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.create(req, uid));
    }

    @PostMapping("/start-chat")
    @Operation(summary = "创建申请并投递到聊天", description = "创建家教申请，同时确保建立会话并发送一条“家教申请”系统消息")
    public BaseResponse<ChatMessageResp> startChat(@Valid @RequestBody CreateTutorApplicationReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.createAndSendToChat(req, uid));
    }

    @GetMapping("/sent/page")
    @Operation(summary = "我发出的申请（游标分页）")
    public BaseResponse<CursorPageResp<TutorApplicationVO>> sent(@Valid TutorApplicationPageReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.listSent(req, uid));
    }

    @GetMapping("/received/page")
    @Operation(summary = "我收到的申请（游标分页）")
    public BaseResponse<CursorPageResp<TutorApplicationVO>> received(@Valid TutorApplicationPageReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.listReceived(req, uid));
    }

    @GetMapping("/unread")
    @Operation(summary = "我收到的申请未读数")
    public BaseResponse<TutorApplicationUnreadResp> unread() {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.unread(uid));
    }

    @GetMapping("/{applicationId}")
    @Operation(summary = "申请详情（接收方查看会自动清未读）")
    public BaseResponse<TutorApplicationVO> detail(@PathVariable("applicationId") Long applicationId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.getDetail(applicationId, uid));
    }

    @PostMapping("/{applicationId}/decision")
    @Operation(summary = "处理申请（通过/拒绝）")
    public BaseResponse<TutorApplicationVO> decide(@PathVariable("applicationId") Long applicationId, @Valid @RequestBody DecideTutorApplicationReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.decide(applicationId, req, uid));
    }

    @PostMapping("/{applicationId}/decision-message")
    @Operation(summary = "处理申请并投递到聊天", description = "处理家教申请，同时发送一条状态变更系统消息；同意时会额外发送中介费支付提示卡片")
    public BaseResponse<ChatMessageResp> decideMessage(@PathVariable("applicationId") Long applicationId, @Valid @RequestBody DecideTutorApplicationReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.decideAndSendToChat(applicationId, req, uid));
    }

    @PostMapping("/{applicationId}/enter-chat")
    @Operation(summary = "进入聊天（含支付gating）")
    public BaseResponse<TutorApplicationEnterResp> enterChat(@PathVariable("applicationId") Long applicationId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(tutorApplicationService.enterChat(applicationId, uid));
    }
}
