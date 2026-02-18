package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
@Tag(name = "实时消息流", description = "提供消息实时推送能力")
public class ChatStreamController {

    @Resource
    private SseSessionManager sseSessionManager;

    @GetMapping("/stream")
    @Operation(summary = "建立实时消息流连接")
    public SseEmitter stream() {
        return sseSessionManager.connect(RequestHolder.get().getUid());
    }
}
