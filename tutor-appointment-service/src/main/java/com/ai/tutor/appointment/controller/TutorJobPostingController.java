package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateTeacherJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateTeacherJobPostingRequest;
import com.ai.tutor.appointment.model.entity.TeacherJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.TeacherJobPostingService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tutor/services")
@Tag(name = "老师服务贴接口", description = "老师发布/管理自己的服务贴，以及浏览公开服务")
public class TutorJobPostingController {

    @Resource
    private TeacherJobPostingService teacherJobPostingService;

    @PostMapping
    @Operation(summary = "发布服务贴")
    public BaseResponse<Long> create(@RequestBody @Valid CreateTeacherJobPostingRequest request) {
        return ResultUtils.success(teacherJobPostingService.create(request, RequestHolder.get().getUid()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新服务贴")
    public BaseResponse<String> update(@PathVariable("id") Long id,
                                      @RequestBody @Valid UpdateTeacherJobPostingRequest request) {
        teacherJobPostingService.update(id, request, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @GetMapping("/{id}")
    @Operation(summary = "服务贴详情")
    public BaseResponse<TeacherJobPosting> detail(@PathVariable("id") Long id) {
        return ResultUtils.success(teacherJobPostingService.getById(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的服务贴列表（游标分页）")
    public BaseResponse<CursorPageResponse<TeacherJobPosting>> mine(@Valid CursorPageRequest request) {
        return ResultUtils.success(teacherJobPostingService.listMine(request, RequestHolder.get().getUid()));
    }

    @GetMapping("/feed")
    @Operation(summary = "服务贴广场列表（游标分页）")
    public BaseResponse<CursorPageResponse<TeacherJobPosting>> feed(@RequestParam(value = "subjectId", required = false) Long subjectId,
                                                                    @RequestParam(value = "city", required = false) String city,
                                                                    @RequestParam(value = "mode", required = false) String mode,
                                                                    @Valid CursorPageRequest request) {
        return ResultUtils.success(teacherJobPostingService.listPublished(subjectId, city, mode, request));
    }
}
