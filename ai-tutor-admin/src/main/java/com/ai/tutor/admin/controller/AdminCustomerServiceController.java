package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.AdminCustomerServiceConfigRequest;
import com.ai.tutor.admin.model.vo.AdminCustomerServiceConfigVO;
import com.ai.tutor.admin.service.AdminCustomerServiceConfigService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/customer-service")
@Tag(name = "Admin Customer Service", description = "人工客服联系方式配置")
public class AdminCustomerServiceController {

    @Resource
    private AdminCustomerServiceConfigService adminCustomerServiceConfigService;

    @GetMapping("/config")
    @Operation(summary = "查询人工客服配置")
    public BaseResponse<AdminCustomerServiceConfigVO> config() {
        return ResultUtils.success(adminCustomerServiceConfigService.config());
    }

    @PostMapping("/config")
    @Operation(summary = "保存人工客服配置")
    public BaseResponse<AdminCustomerServiceConfigVO> save(@RequestBody AdminCustomerServiceConfigRequest request) {
        Long adminUid = RequestHolder.get() == null ? null : RequestHolder.get().getUid();
        return ResultUtils.success(adminCustomerServiceConfigService.save(request, adminUid));
    }

    @PostMapping(value = "/qrcode", consumes = "multipart/form-data")
    @Operation(summary = "上传客服二维码")
    public BaseResponse<AdminCustomerServiceConfigVO> uploadQrCode(@RequestParam("file") MultipartFile file) {
        Long adminUid = RequestHolder.get() == null ? null : RequestHolder.get().getUid();
        return ResultUtils.success(adminCustomerServiceConfigService.uploadQrCode(file, adminUid));
    }
}
