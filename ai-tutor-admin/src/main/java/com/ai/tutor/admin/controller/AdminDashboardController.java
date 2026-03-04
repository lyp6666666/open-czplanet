package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.vo.DashboardStatsResponse;
import com.ai.tutor.admin.service.AdminDashboardService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Dashboard Statistics")
public class AdminDashboardController {

    @Resource
    private AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get Dashboard Stats")
    public BaseResponse<DashboardStatsResponse> getStats() {
        return ResultUtils.success(adminDashboardService.getStats());
    }
}
