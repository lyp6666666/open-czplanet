package com.ai.tutor.liveclass.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.liveclass.domain.vo.request.LiveKitWebhookRequest;
import com.ai.tutor.liveclass.domain.vo.request.SyncCourseSessionRequest;
import com.ai.tutor.liveclass.domain.vo.response.LiveSessionResp;
import com.ai.tutor.liveclass.service.LiveClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/live")
@Tag(name = "实时课堂内部接口", description = "供 appointment/im/media webhook 调用")
public class InternalLiveSessionController {

    @Resource
    private LiveClassService liveClassService;

    @PostMapping("/sessions/sync-from-course")
    @Operation(summary = "课程确认后同步课堂 session")
    public BaseResponse<LiveSessionResp> syncFromCourse(@Valid @RequestBody SyncCourseSessionRequest request) {
        return ResultUtils.success(liveClassService.syncFromCourse(request));
    }

    @PostMapping("/webhooks/livekit")
    @Operation(summary = "LiveKit webhook")
    public BaseResponse<Boolean> liveKitWebhook(@RequestBody LiveKitWebhookRequest request) {
        liveClassService.consumeLiveKitWebhook(request);
        return ResultUtils.success(Boolean.TRUE);
    }
}
