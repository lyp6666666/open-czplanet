package com.ai.tutor.liveclass.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.liveclass.domain.vo.request.EndLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.IssueJoinTokenRequest;
import com.ai.tutor.liveclass.domain.vo.request.LeaveLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.LiveDeviceReportRequest;
import com.ai.tutor.liveclass.domain.vo.request.PrepareLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.response.IssueJoinTokenResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveSessionResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveTimelineItemResp;
import com.ai.tutor.liveclass.domain.vo.response.PrepareLiveSessionResp;
import com.ai.tutor.liveclass.service.LiveClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @GetMapping("/sessions/by-course/{courseId}")
    @Operation(summary = "按课程查询课堂信息")
    public BaseResponse<LiveSessionResp> getByCourse(@PathVariable("courseId") Long courseId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.getByCourseId(courseId, uid));
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
                                                      @Valid @RequestBody IssueJoinTokenRequest request) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.issueJoinToken(sessionId, uid, request));
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(summary = "查询课堂状态")
    public BaseResponse<LiveSessionResp> status(@PathVariable("sessionId") Long sessionId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(liveClassService.status(sessionId, uid));
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
}
