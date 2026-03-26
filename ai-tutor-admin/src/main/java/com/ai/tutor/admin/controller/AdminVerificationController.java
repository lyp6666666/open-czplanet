package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.VerificationAuditRequest;
import com.ai.tutor.admin.model.entity.TeacherProfile;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminVerificationService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/verification")
@Tag(name = "Admin Verification", description = "Teacher Verification Review")
public class AdminVerificationController {

    @Resource
    private AdminVerificationService adminVerificationService;

    @GetMapping("/pending")
    @Operation(summary = "List Pending Verifications")
    public BaseResponse<PageResult<TeacherProfile>> listPendingVerifications(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                             @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultUtils.success(adminVerificationService.listPendingVerifications(page, size));
    }

    @GetMapping("/details/{userId}")
    @Operation(summary = "Get Verification Details")
    public BaseResponse<TeacherProfile> getVerificationDetails(@PathVariable("userId") Long userId) {
        return ResultUtils.success(adminVerificationService.getVerificationDetails(userId));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve Verification")
    public BaseResponse<Boolean> approveVerification(@RequestBody VerificationAuditRequest request) {
        adminVerificationService.approveVerification(request.getUserId(), request.getType());
        return ResultUtils.success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject Verification")
    public BaseResponse<Boolean> rejectVerification(@RequestBody VerificationAuditRequest request) {
        adminVerificationService.rejectVerification(request.getUserId(), request.getType(), request.getReason());
        return ResultUtils.success(true);
    }
}
