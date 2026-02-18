package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.config.OpsProperties;
import com.ai.tutor.appointment.enums.VerificationTypeEnum;
import com.ai.tutor.appointment.model.dto.verification.OpsTeacherVerificationAuditRequest;
import com.ai.tutor.appointment.service.TeacherVerificationService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/ops/teacher/verification")
@Tag(name = "运营审核接口", description = "教师实名认证/学历认证审核接口")
public class OpsTeacherVerificationController {

    @Resource
    private OpsProperties opsProperties;

    @Resource
    private TeacherVerificationService teacherVerificationService;

    @PostMapping("/approve")
    @Operation(summary = "审核通过")
    public BaseResponse<Boolean> approve(@RequestBody OpsTeacherVerificationAuditRequest req,
                                         @RequestHeader(value = "X-Ops-Token", required = false) String token) {
        check(token);
        ThrowUtils.throwIf(req == null || req.getUserId() == null, ErrorCode.PARAMS_ERROR);
        VerificationTypeEnum type = VerificationTypeEnum.fromCode(req.getType());
        teacherVerificationService.opsApprove(req.getUserId(), type);
        return ResultUtils.success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "审核驳回")
    public BaseResponse<Boolean> reject(@RequestBody OpsTeacherVerificationAuditRequest req,
                                        @RequestHeader(value = "X-Ops-Token", required = false) String token) {
        check(token);
        ThrowUtils.throwIf(req == null || req.getUserId() == null, ErrorCode.PARAMS_ERROR);
        VerificationTypeEnum type = VerificationTypeEnum.fromCode(req.getType());
        teacherVerificationService.opsReject(req.getUserId(), type, req.getReason());
        return ResultUtils.success(true);
    }

    private void check(String token) {
        String expected = opsProperties == null ? null : opsProperties.getVerifyToken();
        ThrowUtils.throwIf(expected == null || expected.isBlank(), ErrorCode.NO_AUTH_ERROR, "ops token not configured");
        ThrowUtils.throwIf(token == null || !token.equals(expected), ErrorCode.NO_AUTH_ERROR, "ops token invalid");
    }
}

