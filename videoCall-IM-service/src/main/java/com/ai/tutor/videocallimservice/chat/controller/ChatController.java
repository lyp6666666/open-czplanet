package com.ai.tutor.videocallimservice.chat.controller;


import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessagePageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.common.annotation.FrequencyControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@Tag(name = "聊天室接口", description = "提供用户发送接受消息相关接口")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @GetMapping("/public/msg/page")
    @Operation(summary = "消息列表")
    @FrequencyControl(time = 120, count = 20, target = FrequencyControl.Target.IP)
    public BaseResponse<CursorPageBaseResp<ChatMessageResp>> getMsgPage(@Valid ChatMessagePageReq request) {
        CursorPageBaseResp<ChatMessageResp> msgPage = chatService.getMsgPage(request, RequestHolder.get().getUid());
        //filterBlackMsg(msgPage);
        return ResultUtils.success(msgPage);
    }



    @PostMapping("/msg")
    @Operation(summary = "发送消息")
    @FrequencyControl(time = 5, count = 3, target = FrequencyControl.Target.UID)
    @FrequencyControl(time = 30, count = 5, target = FrequencyControl.Target.UID)
    @FrequencyControl(time = 60, count = 10, target = FrequencyControl.Target.UID)
    public BaseResponse<ChatMessageResp> sendMsg(@Valid @RequestBody ChatMessageReq request) {
//        Long msgId = chatService.sendMsg(request, RequestHolder.get().getUid());
//        //返回完整消息格式，方便前端展示
//        return ApiResult.success(chatService.getMsgResp(msgId, RequestHolder.get().getUid()));
        return null;
    }

}
