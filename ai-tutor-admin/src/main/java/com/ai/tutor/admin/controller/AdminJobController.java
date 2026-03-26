package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.RejectJobRequest;
import com.ai.tutor.admin.model.entity.StudentJobPosting;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminJobService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/jobs")
@Tag(name = "Admin Job Management", description = "Student Job Approval")
public class AdminJobController {

    @Resource
    private AdminJobService adminJobService;

    @GetMapping("/pending")
    @Operation(summary = "List Pending Jobs")
    public BaseResponse<PageResult<StudentJobPosting>> listPendingJobs(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                       @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultUtils.success(adminJobService.listPendingJobs(page, size));
    }

    @PostMapping("/approve/{id}")
    @Operation(summary = "Approve Job")
    public BaseResponse<Boolean> approveJob(@PathVariable("id") Long id) {
        adminJobService.approveJob(id);
        return ResultUtils.success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject Job")
    public BaseResponse<Boolean> rejectJob(@RequestBody RejectJobRequest request) {
        adminJobService.rejectJob(request.getId(), request.getReason());
        return ResultUtils.success(true);
    }
}
