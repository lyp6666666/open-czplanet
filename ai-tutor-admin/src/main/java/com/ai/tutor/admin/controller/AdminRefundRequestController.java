package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.admin.model.vo.AdminRefundDecisionRequest;
import com.ai.tutor.admin.model.vo.AdminRefundRejectRequest;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.model.vo.RefundRequestDetailResponse;
import com.ai.tutor.admin.service.AdminRefundRequestService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refund/requests")
@Tag(name = "退款申请管理", description = "管理端查看退款申请与审核")
public class AdminRefundRequestController {

    @Resource
    private AdminRefundRequestService adminRefundRequestService;

    @GetMapping
    @Operation(summary = "退款申请列表")
    public BaseResponse<PageResult<RefundRequestRecord>> list(@RequestParam(value = "page", required = false) Integer page,
                                                              @RequestParam(value = "size", required = false) Integer size,
                                                              @RequestParam(value = "type", required = false) String type,
                                                              @RequestParam(value = "status", required = false) String status) {
        int p = page == null ? 1 : page;
        int s = size == null ? 20 : size;
        return ResultUtils.success(adminRefundRequestService.list(p, s, type, status));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "退款申请详情（含聊天记录与证据）")
    public BaseResponse<RefundRequestDetailResponse> detail(@PathVariable("requestId") Long requestId) {
        return ResultUtils.success(adminRefundRequestService.detail(requestId));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "审核通过并发起原路退款（幂等）")
    public BaseResponse<Boolean> approve(@PathVariable("requestId") Long requestId,
                                         @RequestBody(required = false) AdminRefundDecisionRequest body) {
        Long adminUid = RequestHolder.get().getUid();
        String note = body == null ? null : body.getNote();
        adminRefundRequestService.approve(requestId, adminUid, note);
        return ResultUtils.success(true);
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "审核拒绝（幂等）")
    public BaseResponse<Boolean> reject(@PathVariable("requestId") Long requestId,
                                        @Valid @RequestBody AdminRefundRejectRequest body) {
        Long adminUid = RequestHolder.get().getUid();
        adminRefundRequestService.reject(requestId, adminUid, body.getReason());
        return ResultUtils.success(true);
    }
}

