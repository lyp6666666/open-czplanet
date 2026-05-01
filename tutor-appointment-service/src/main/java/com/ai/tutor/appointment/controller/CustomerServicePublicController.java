package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.CustomerServiceConfigVO;
import com.ai.tutor.appointment.service.CustomerServiceConfigService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/customer-service")
@Tag(name = "公开客服配置", description = "人工客服联系方式公开配置")
public class CustomerServicePublicController {

    @Resource
    private CustomerServiceConfigService customerServiceConfigService;

    @GetMapping("/config")
    @Operation(summary = "获取人工客服联系方式")
    public BaseResponse<CustomerServiceConfigVO> config() {
        return ResultUtils.success(customerServiceConfigService.config());
    }
}
