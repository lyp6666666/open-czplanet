package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateDirectBrokerageOrderReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SubmitBrokerageProofReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.BrokerageOrderVO;
import com.ai.tutor.videocallimservice.chat.service.BrokerageOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/brokerage")
@Tag(name = "中介费订单接口", description = "合作同意后的中介费支付与状态")
public class BrokerageOrderController {

    @Resource
    private BrokerageOrderService brokerageOrderService;

    @PostMapping("/order/direct")
    @Operation(summary = "创建直接支付订单（测试/预约用）")
    public BaseResponse<BrokerageOrderVO> createDirect(@Valid @RequestBody CreateDirectBrokerageOrderReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(brokerageOrderService.createDirectOrder(req, uid));
    }

    @PostMapping("/order/by-proposal/{proposalId}")
    @Operation(summary = "按合作提案创建/获取订单（幂等）")
    public BaseResponse<BrokerageOrderVO> getOrCreateByProposal(@PathVariable("proposalId") Long proposalId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(brokerageOrderService.getOrCreateByProposal(proposalId, uid));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单")
    public BaseResponse<BrokerageOrderVO> get(@PathVariable("orderId") Long orderId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(brokerageOrderService.getById(orderId, uid));
    }

    @PostMapping("/order/{orderId}/submit-proof")
    @Operation(summary = "提交支付凭证")
    public BaseResponse<BrokerageOrderVO> submitProof(@PathVariable("orderId") Long orderId, @Valid @RequestBody SubmitBrokerageProofReq req) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(brokerageOrderService.submitProof(orderId, req, uid));
    }

    @PostMapping("/order/{orderId}/cancel")
    @Operation(summary = "撤单（撤销支付）")
    public BaseResponse<BrokerageOrderVO> cancel(@PathVariable("orderId") Long orderId) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(brokerageOrderService.cancel(orderId, uid));
    }

    @PostMapping("/admin/order/{orderId}/mark-paid")
    @Operation(summary = "管理端确认到账")
    public BaseResponse<BrokerageOrderVO> markPaid(@PathVariable("orderId") Long orderId, @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        return ResultUtils.success(brokerageOrderService.markPaid(orderId, token));
    }
}
