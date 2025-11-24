package com.ai.tutor.videocallimservice.chat.controller;


import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ApiResult;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.common.annotation.FrequencyControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@Tag(name = "聊天室接口", description = "提供用户发送接受消息相关接口")
@Slf4j
public class ChatController {

//    @Resource
//    private ChatService chatService;

    @PostMapping("/msg")
    @Operation(summary = "发送消息")
    @FrequencyControl(time = 5, count = 3, target = FrequencyControl.Target.UID)
    @FrequencyControl(time = 30, count = 5, target = FrequencyControl.Target.UID)
    @FrequencyControl(time = 60, count = 10, target = FrequencyControl.Target.UID)
    public ApiResult<ChatMessageResp> sendMsg(@Valid @RequestBody ChatMessageReq request) {
//        Long msgId = chatService.sendMsg(request, RequestHolder.get().getUid());
//        //返回完整消息格式，方便前端展示
//        return ApiResult.success(chatService.getMsgResp(msgId, RequestHolder.get().getUid()));
        return null;
    }

}
