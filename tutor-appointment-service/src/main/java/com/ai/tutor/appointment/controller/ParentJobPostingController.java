package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.StudentJobPostingService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parent/jobs")
@Tag(name = "家长需求贴接口", description = "家长发布/管理自己的需求贴，以及浏览公开需求")
public class ParentJobPostingController {

    @Resource
    private StudentJobPostingService studentJobPostingService;

    @PostMapping
    @Operation(summary = "发布需求贴")
    public BaseResponse<Long> create(@RequestBody @Valid CreateStudentJobPostingRequest request) {
        return ResultUtils.success(studentJobPostingService.create(request, RequestHolder.get().getUid()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新需求贴")
    public BaseResponse<String> update(@PathVariable("id") Long id,
                                      @RequestBody @Valid UpdateStudentJobPostingRequest request) {
        studentJobPostingService.update(id, request, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @GetMapping("/{id}")
    @Operation(summary = "需求贴详情")
    public BaseResponse<StudentJobPosting> detail(@PathVariable("id") Long id) {
        return ResultUtils.success(studentJobPostingService.getById(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的需求贴列表（游标分页）")
    public BaseResponse<CursorPageResponse<StudentJobPosting>> mine(@Valid CursorPageRequest request) {
        return ResultUtils.success(studentJobPostingService.listMine(request, RequestHolder.get().getUid()));
    }

    @GetMapping("/feed")
    @Operation(summary = "需求贴广场列表（游标分页）")
    public BaseResponse<CursorPageResponse<StudentJobPosting>> feed(@RequestParam(value = "subjectId", required = false) Long subjectId,
                                                                    @RequestParam(value = "city", required = false) String city,
                                                                    @RequestParam(value = "classMode", required = false) String classMode,
                                                                    @Valid CursorPageRequest request) {
        return ResultUtils.success(studentJobPostingService.listPublished(subjectId, city, classMode, request));
    }
}
