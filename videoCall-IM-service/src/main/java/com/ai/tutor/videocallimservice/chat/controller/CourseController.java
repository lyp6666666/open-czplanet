package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ApplyTrialRefundReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseDetailVO;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CourseItemVO;
import com.ai.tutor.videocallimservice.chat.service.CourseEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@Tag(name = "我的课程接口", description = "课程状态列表与试课退款申请")
public class CourseController {

    @Resource
    private CourseEnrollmentService courseEnrollmentService;

    @GetMapping("/my")
    @Operation(summary = "我的课程列表")
    public BaseResponse<List<CourseItemVO>> myCourses(@RequestParam(value = "page", required = false) Integer page,
                                                      @RequestParam(value = "size", required = false) Integer size,
                                                      @RequestParam(value = "role", required = false) String role) {
        Long uid = RequestHolder.get().getUid();
        String r = role == null ? "" : role.trim();
        int p = page == null ? 1 : page;
        int s = size == null ? 20 : size;
        return ResultUtils.success(courseEnrollmentService.listMyCourses(uid, r, p, s));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "长期课程详情")
    public BaseResponse<CourseDetailVO> detail(@PathVariable("courseId") Long courseId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(courseEnrollmentService.getCourseDetail(courseId, uid));
    }

    @GetMapping("/by-room/{roomId}")
    @Operation(summary = "按会话查询当前长期课程")
    public BaseResponse<CourseDetailVO> byRoom(@PathVariable("roomId") Long roomId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(courseEnrollmentService.getCourseByRoom(roomId, uid));
    }

    @PostMapping("/{courseId}/trial-refund/apply")
    @Operation(summary = "试课不通过退款申请（退 60% 信息费）")
    public BaseResponse<Long> applyTrialRefund(@PathVariable("courseId") Long courseId,
                                               @Valid @RequestBody ApplyTrialRefundReq request) {
        Long uid = RequestHolder.get().getUid();
        Long id = courseEnrollmentService.applyTrialRefund(courseId, request, uid);
        return ResultUtils.success(id);
    }
}
