package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.StudentJobPostingService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/org/jobs")
@Tag(name = "机构需求接口", description = "机构发布与管理需求")
public class OrganizationJobPostingController {

    @Resource
    private StudentJobPostingService studentJobPostingService;

    @PostMapping
    @Operation(summary = "机构发布需求（创建）")
    public BaseResponse<Long> create(@RequestBody CreateStudentJobPostingRequest request) {
        ThrowUtils.throwIf(RequestHolder.get() == null || RequestHolder.get().getUid() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(RequestHolder.get().getRole() == null || RequestHolder.get().getRole() != UserRoleEnum.ORG.getValue(), ErrorCode.NO_AUTH_ERROR);
        request.setPublisherIdentity("ORGANIZATION");
        return ResultUtils.success(studentJobPostingService.create(request, RequestHolder.get().getUid()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "机构需求更新")
    public BaseResponse<String> update(@PathVariable("id") Long id, @RequestBody UpdateStudentJobPostingRequest request) {
        ThrowUtils.throwIf(RequestHolder.get() == null || RequestHolder.get().getUid() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(RequestHolder.get().getRole() == null || RequestHolder.get().getRole() != UserRoleEnum.ORG.getValue(), ErrorCode.NO_AUTH_ERROR);
        studentJobPostingService.update(id, request, RequestHolder.get().getUid());
        return ResultUtils.success("OK");
    }

    @GetMapping("/mine")
    @Operation(summary = "机构我发布的需求列表")
    public BaseResponse<CursorPageResponse<StudentJobPosting>> mine(@Valid CursorPageRequest request) {
        ThrowUtils.throwIf(RequestHolder.get() == null || RequestHolder.get().getUid() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(RequestHolder.get().getRole() == null || RequestHolder.get().getRole() != UserRoleEnum.ORG.getValue(), ErrorCode.NO_AUTH_ERROR);
        return ResultUtils.success(studentJobPostingService.listMine(request, RequestHolder.get().getUid()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取机构需求详情")
    public BaseResponse<StudentJobPosting> get(@PathVariable("id") Long id) {
        ThrowUtils.throwIf(RequestHolder.get() == null || RequestHolder.get().getUid() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(RequestHolder.get().getRole() == null || RequestHolder.get().getRole() != UserRoleEnum.ORG.getValue(), ErrorCode.NO_AUTH_ERROR);
        StudentJobPosting posting = studentJobPostingService.getById(id);
        ThrowUtils.throwIf(posting == null || !RequestHolder.get().getUid().equals(posting.getParentId()), ErrorCode.NO_AUTH_ERROR);
        return ResultUtils.success(posting);
    }
}
