package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.RefundAuditRequest;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.vo.DisputeDetailResponse;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminRefundService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refund")
@Tag(name = "Admin Refund Management", description = "Refund Dispute Resolution")
public class AdminRefundController {

    @Resource
    private AdminRefundService adminRefundService;

    @GetMapping("/disputes")
    @Operation(summary = "List Refund Disputes")
    public BaseResponse<PageResult<BrokerageOrder>> listRefundDisputes(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                       @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultUtils.success(adminRefundService.listRefundDisputes(page, size));
    }

    @GetMapping("/details/{orderId}")
    @Operation(summary = "Get Dispute Details")
    public BaseResponse<DisputeDetailResponse> getDisputeDetails(@PathVariable("orderId") Long orderId) {
        return ResultUtils.success(adminRefundService.getDisputeDetails(orderId));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve Refund")
    public BaseResponse<Boolean> approveRefund(@RequestBody RefundAuditRequest request) {
        adminRefundService.approveRefund(request.getOrderId());
        return ResultUtils.success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject Refund")
    public BaseResponse<Boolean> rejectRefund(@RequestBody RefundAuditRequest request) {
        adminRefundService.rejectRefund(request.getOrderId(), request.getReason());
        return ResultUtils.success(true);
    }
}
