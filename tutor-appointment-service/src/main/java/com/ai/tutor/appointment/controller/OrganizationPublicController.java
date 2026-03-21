package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import com.ai.tutor.appointment.service.OrganizationPublicService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/organization")
@Tag(name = "机构公开接口", description = "教师端可访问的机构主页信息")
public class OrganizationPublicController {

    @Resource
    private OrganizationPublicService organizationPublicService;

    @GetMapping("/{orgUserId}")
    @Operation(summary = "获取机构主页信息")
    public BaseResponse<OrganizationProfile> get(@PathVariable("orgUserId") Long orgUserId) {
        return ResultUtils.success(organizationPublicService.getByOrgUserId(orgUserId));
    }
}
