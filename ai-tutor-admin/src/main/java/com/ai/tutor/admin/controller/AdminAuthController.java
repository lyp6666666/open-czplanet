package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.AdminLoginRequest;
import com.ai.tutor.admin.model.vo.AdminLoginResponse;
import com.ai.tutor.admin.service.AdminAuthService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Authentication", description = "Admin Login")
public class AdminAuthController {

    @Resource
    private AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(summary = "Admin Login")
    public BaseResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        return ResultUtils.success(adminAuthService.login(request));
    }
}
