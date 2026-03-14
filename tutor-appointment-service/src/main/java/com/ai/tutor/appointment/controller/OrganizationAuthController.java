package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.model.dto.organization.OrgChangePasswordRequest;
import com.ai.tutor.appointment.model.dto.organization.OrgLoginRequest;
import com.ai.tutor.appointment.model.vo.OrgLoginVO;
import com.ai.tutor.appointment.service.OrganizationAuthService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/org/auth")
@Tag(name = "机构登录接口", description = "机构账号登录与改密")
public class OrganizationAuthController {

    @Resource
    private OrganizationAuthService organizationAuthService;

    @PostMapping("/login")
    @Operation(summary = "机构账号登录")
    public BaseResponse<OrgLoginVO> login(@RequestBody OrgLoginRequest request) {
        return ResultUtils.success(organizationAuthService.login(request));
    }

    @PostMapping("/changePassword")
    @Operation(summary = "机构账号修改密码")
    public BaseResponse<String> changePassword(@RequestBody OrgChangePasswordRequest request) {
        ThrowUtils.throwIf(RequestHolder.get() == null || RequestHolder.get().getUid() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(RequestHolder.get().getRole() == null || RequestHolder.get().getRole() != UserRoleEnum.ORG.getValue(), ErrorCode.NO_AUTH_ERROR, "仅机构账号可操作");
        organizationAuthService.changePassword(RequestHolder.get().getUid(), request);
        return ResultUtils.success("OK");
    }
}
