package com.ai.tutor.liveclass.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.liveclass.domain.vo.request.EndLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.IssueJoinTokenRequest;
import com.ai.tutor.liveclass.domain.vo.request.JoinLiveSessionAckRequest;
import com.ai.tutor.liveclass.domain.vo.request.LeaveLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.LiveAiAudioChunkRequest;
import com.ai.tutor.liveclass.domain.vo.request.LiveDeviceReportRequest;
import com.ai.tutor.liveclass.domain.vo.request.LiveWhiteboardSnapshotRequest;
import com.ai.tutor.liveclass.domain.vo.request.PrepareLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.UpdateLiveAiOptionsRequest;
import com.ai.tutor.liveclass.domain.vo.response.IssueJoinTokenResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiResultResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiStateResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveReminderItemResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveSessionResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveTimelineItemResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveWhiteboardSnapshotResp;
import com.ai.tutor.liveclass.domain.vo.response.PrepareLiveSessionResp;
import com.ai.tutor.liveclass.service.LiveClassService;
import com.ai.tutor.liveclass.service.LiveKitUrlResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/live")
@Tag(name = "实时课堂接口", description = "课堂详情、准备页、入会、设备上报和结束课堂")
public class LiveSessionController {

    @Resource
    private LiveClassService liveClassService;
    @Resource
    private LiveKitUrlResolver liveKitUrlResolver;

    @GetMapping("/sessions/by-course/{courseId}")
    @Operation(summary = "按课程查询课堂信息")
    public BaseResponse<LiveSessionResp> getByCourse(@PathVariable("courseId") Long courseId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.getByCourseId(courseId, uid));
    }

    @GetMapping("/sessions/reminders")
    @Operation(summary = "查询当前用户的课堂提醒列表")
    public BaseResponse<List<LiveReminderItemResp>> reminders() {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.myReminders(uid));
    }

    @PostMapping("/sessions/by-course/{courseId}/prepare")
    @Operation(summary = "进入设备检测页前的课堂准备信息")
    public BaseResponse<PrepareLiveSessionResp> prepare(@PathVariable("courseId") Long courseId,
                                                        @Valid @RequestBody PrepareLiveSessionRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.prepare(courseId, uid, request));
    }

    @PostMapping("/sessions/{sessionId}/join-token")
    @Operation(summary = "签发实时课堂入会 token")
    public BaseResponse<IssueJoinTokenResp> joinToken(@PathVariable("sessionId") Long sessionId,
                                                      @Valid @RequestBody IssueJoinTokenRequest request,
                                                      HttpServletRequest httpServletRequest) {
        Long uid = RequestHolder.get().getUid();
        String publicWsUrl = liveKitUrlResolver.resolvePublicWsUrl(httpServletRequest);
        return ResultUtils.success(liveClassService.issueJoinToken(sessionId, uid, request, publicWsUrl));
    }

    @PostMapping("/sessions/{sessionId}/join-ack")
    @Operation(summary = "客户端完成入会后的确认上报")
    public BaseResponse<LiveSessionResp> joinAck(@PathVariable("sessionId") Long sessionId,
                                                 @RequestBody(required = false) JoinLiveSessionAckRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.joinAck(sessionId, uid, request == null ? new JoinLiveSessionAckRequest() : request));
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(summary = "查询课堂状态")
    public BaseResponse<LiveSessionResp> status(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.status(sessionId, uid));
    }

    @GetMapping("/sessions/{sessionId}/ai/state")
    @Operation(summary = "查询课堂 AI 当前状态")
    public BaseResponse<LiveAiStateResp> aiState(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.aiState(sessionId, uid));
    }

    @PostMapping("/sessions/{sessionId}/ai/options")
    @Operation(summary = "更新课堂 AI 实时总结与课后总结选项")
    public BaseResponse<LiveSessionResp> updateAiOptions(@PathVariable("sessionId") Long sessionId,
                                                         @RequestBody(required = false) UpdateLiveAiOptionsRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.updateAiOptions(sessionId, uid, request == null ? new UpdateLiveAiOptionsRequest() : request));
    }

    @PostMapping("/sessions/{sessionId}/ai/audio-chunks")
    @Operation(summary = "上传课堂 AI 旁路音频分片")
    public BaseResponse<LiveAiStateResp> uploadAiAudioChunk(@PathVariable("sessionId") Long sessionId,
                                                            @RequestBody LiveAiAudioChunkRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.acceptAiAudioChunk(sessionId, uid, request));
    }

    @GetMapping("/sessions/{sessionId}/ai/result")
    @Operation(summary = "查询课堂 AI 课后结果")
    public BaseResponse<LiveAiResultResp> aiResult(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.aiResult(sessionId, uid));
    }

    @PostMapping("/sessions/{sessionId}/ai/result/retry")
    @Operation(summary = "重试生成课堂 AI 课后结果")
    public BaseResponse<LiveAiResultResp> retryAiResult(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.retryAiResult(sessionId, uid));
    }

    @PostMapping("/sessions/{sessionId}/device-report")
    @Operation(summary = "设备检测上报")
    public BaseResponse<Boolean> deviceReport(@PathVariable("sessionId") Long sessionId,
                                              @RequestBody LiveDeviceReportRequest request) {
        Long uid = RequestHolder.get().getUid();
        liveClassService.reportDevice(sessionId, uid, request);
        return ResultUtils.success(Boolean.TRUE);
    }

    @PostMapping("/sessions/{sessionId}/leave")
    @Operation(summary = "离开课堂")
    public BaseResponse<LiveSessionResp> leave(@PathVariable("sessionId") Long sessionId,
                                               @RequestBody(required = false) LeaveLiveSessionRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.leave(sessionId, uid, request == null ? new LeaveLiveSessionRequest() : request));
    }

    @PostMapping("/sessions/{sessionId}/end")
    @Operation(summary = "结束课堂")
    public BaseResponse<LiveSessionResp> end(@PathVariable("sessionId") Long sessionId,
                                             @RequestBody(required = false) EndLiveSessionRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.end(sessionId, uid, request == null ? new EndLiveSessionRequest() : request));
    }

    @GetMapping("/sessions/{sessionId}/timeline")
    @Operation(summary = "课堂时间线")
    public BaseResponse<List<LiveTimelineItemResp>> timeline(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.timeline(sessionId, uid));
    }

    @GetMapping("/sessions/{sessionId}/whiteboard")
    @Operation(summary = "查询或创建本节课白板快照")
    public BaseResponse<LiveWhiteboardSnapshotResp> whiteboard(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.whiteboard(sessionId, uid));
    }

    @PutMapping("/sessions/{sessionId}/whiteboard/snapshot")
    @Operation(summary = "保存本节课白板快照")
    public BaseResponse<LiveWhiteboardSnapshotResp> saveWhiteboard(@PathVariable("sessionId") Long sessionId,
                                                                   @RequestBody(required = false) LiveWhiteboardSnapshotRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.saveWhiteboard(sessionId, uid, request == null ? new LiveWhiteboardSnapshotRequest() : request, false));
    }

    @PostMapping("/sessions/{sessionId}/whiteboard/finalize")
    @Operation(summary = "结束课堂前保存并归档白板快照")
    public BaseResponse<LiveWhiteboardSnapshotResp> finalizeWhiteboard(@PathVariable("sessionId") Long sessionId,
                                                                       @RequestBody(required = false) LiveWhiteboardSnapshotRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.saveWhiteboard(sessionId, uid, request == null ? new LiveWhiteboardSnapshotRequest() : request, true));
    }
}
