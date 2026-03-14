package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.mapper.OrganizationProfileMapper;
import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/organization")
@Tag(name = "机构公开接口", description = "教师端可访问的机构主页信息")
public class OrganizationPublicController {

    @Resource
    private OrganizationProfileMapper organizationProfileMapper;

    @GetMapping("/{orgUserId}")
    @Operation(summary = "获取机构主页信息")
    public BaseResponse<OrganizationProfile> get(@PathVariable("orgUserId") Long orgUserId) {
        ThrowUtils.throwIf(orgUserId == null, ErrorCode.PARAMS_ERROR);
        OrganizationProfile profile = organizationProfileMapper.selectByUserId(orgUserId);
        ThrowUtils.throwIf(profile == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(profile);
    }
}
