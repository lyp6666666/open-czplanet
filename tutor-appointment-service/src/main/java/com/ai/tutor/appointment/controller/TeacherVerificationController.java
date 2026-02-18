package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.verification.SubmitEducationVerificationRequest;
import com.ai.tutor.appointment.model.dto.verification.SubmitRealnameVerificationRequest;
import com.ai.tutor.appointment.service.TeacherVerificationService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.ai.tutor.utils.RequestHolder.ATTRIBUTE_UID;

@RestController
@RequestMapping("/teacher/verification")
@Tag(name = "教师认证接口", description = "教师实名认证/学历认证提交接口")
public class TeacherVerificationController {

    @Resource
    private TeacherVerificationService teacherVerificationService;

    @PostMapping("/education/submit")
    @Operation(summary = "提交学历认证")
    public BaseResponse<Boolean> submitEducation(@Valid @RequestBody SubmitEducationVerificationRequest req, HttpServletRequest request) {
        String uidStr = (String) request.getAttribute(ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        teacherVerificationService.submitEducation(Long.parseLong(uidStr), req.getProofUrls());
        return ResultUtils.success(true);
    }

    @PostMapping("/realname/submit")
    @Operation(summary = "提交实名认证")
    public BaseResponse<Boolean> submitRealname(@RequestBody SubmitRealnameVerificationRequest req, HttpServletRequest request) {
        String uidStr = (String) request.getAttribute(ATTRIBUTE_UID);
        ThrowUtils.throwIf(uidStr == null, ErrorCode.NOT_LOGIN_ERROR);
        String method = req == null ? null : req.getMethod();
        ThrowUtils.throwIf(method == null || method.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "请选择提交方式");
        String m = method.trim().toUpperCase();
        Long uid = Long.parseLong(uidStr);
        if ("ID_PHOTO".equals(m)) {
            teacherVerificationService.submitRealnameIdPhoto(uid, req.getIdFrontUrl(), req.getIdBackUrl());
            return ResultUtils.success(true);
        }
        if ("NAME_IDNO".equals(m)) {
            teacherVerificationService.submitRealnameNameIdno(uid, req.getRealName(), req.getIdNo());
            return ResultUtils.success(true);
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "未知提交方式");
        return ResultUtils.success(false);
    }
}

