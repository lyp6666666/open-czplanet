package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RealtimeEventSyncResp;
import com.ai.tutor.videocallimservice.chat.service.realtime.RealtimeEventStoreService;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
@Tag(name = "实时消息流", description = "提供消息实时推送能力")
public class ChatStreamController {

    @Resource
    private SseSessionManager sseSessionManager;

    @Resource
    private RealtimeEventStoreService realtimeEventStoreService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立实时消息流连接")
    public SseEmitter stream() {
        return sseSessionManager.connect(RequestHolder.get().getUid());
    }

    @GetMapping(value = "/stream/v2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立实时消息流连接 v2，支持心跳与短窗口补偿")
    public SseEmitter streamV2(@RequestParam(value = "clientId", required = false) String clientId,
                               @RequestParam(value = "lastEventId", required = false) Long lastEventId) {
        return sseSessionManager.connectV2(RequestHolder.get().getUid(), clientId, lastEventId);
    }

    @GetMapping("/events/sync")
    @Operation(summary = "按事件水位补偿同步实时事件")
    public BaseResponse<RealtimeEventSyncResp> sync(@RequestParam(value = "lastEventId", required = false) Long lastEventId,
                                                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ResultUtils.success(realtimeEventStoreService.sync(RequestHolder.get().getUid(), lastEventId, pageSize));
    }
}
