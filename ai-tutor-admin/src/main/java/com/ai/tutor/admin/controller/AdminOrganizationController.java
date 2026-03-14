package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.AdminOrganizationCreateRequest;
import com.ai.tutor.admin.model.dto.AdminOrganizationUpdateRequest;
import com.ai.tutor.admin.model.vo.AdminOrganizationCreateResponse;
import com.ai.tutor.admin.model.vo.AdminOrganizationDetailVO;
import com.ai.tutor.admin.model.vo.AdminOrganizationRowVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminOrganizationService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/organizations")
@Tag(name = "Admin Organization", description = "Organization CRUD")
public class AdminOrganizationController {

    @Resource
    private AdminOrganizationService adminOrganizationService;

    @PostMapping
    @Operation(summary = "Create Organization")
    public BaseResponse<AdminOrganizationCreateResponse> create(@RequestBody AdminOrganizationCreateRequest request) {
        return ResultUtils.success(adminOrganizationService.create(request));
    }

    @GetMapping
    @Operation(summary = "Page Organizations")
    public BaseResponse<PageResult<AdminOrganizationRowVO>> page(@RequestParam(value = "q", required = false) String q,
                                                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultUtils.success(adminOrganizationService.page(q, page, size));
    }

    @GetMapping("/{orgUserId}")
    @Operation(summary = "Get Organization Detail")
    public BaseResponse<AdminOrganizationDetailVO> getDetail(@PathVariable("orgUserId") Long orgUserId) {
        return ResultUtils.success(adminOrganizationService.getDetail(orgUserId));
    }

    @PutMapping("/{orgUserId}")
    @Operation(summary = "Update Organization")
    public BaseResponse<Boolean> update(@PathVariable("orgUserId") Long orgUserId,
                                        @RequestBody AdminOrganizationUpdateRequest request) {
        adminOrganizationService.update(orgUserId, request);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/{orgUserId}")
    @Operation(summary = "Disable Organization")
    public BaseResponse<Boolean> disable(@PathVariable("orgUserId") Long orgUserId) {
        adminOrganizationService.disable(orgUserId);
        return ResultUtils.success(true);
    }
}
