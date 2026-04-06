package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.integration.feign.PaymentRefundFeignClient;
import com.ai.tutor.admin.mapper.AdminMessageMapper;
import com.ai.tutor.admin.mapper.AdminRefundMapper;
import com.ai.tutor.admin.mapper.AdminRefundRequestMapper;
import com.ai.tutor.admin.model.dto.PaymentRefundRequest;
import com.ai.tutor.admin.model.dto.PaymentRefundResponse;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.entity.Message;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.model.vo.RefundRequestDetailResponse;
import com.ai.tutor.admin.service.AdminRefundRequestService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminRefundRequestServiceImpl implements AdminRefundRequestService {

    @Resource
    private AdminRefundRequestMapper adminRefundRequestMapper;
    @Resource
    private AdminRefundMapper adminRefundMapper;
    @Resource
    private AdminMessageMapper adminMessageMapper;
    @Resource
    private PaymentRefundFeignClient paymentRefundFeignClient;

    @Override
    public PageResult<RefundRequestRecord> list(int page, int size, String type, String status) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size <= 0 ? 20 : size, 50));
        long offset = (long) (p - 1) * s;
        List<RefundRequestRecord> records = adminRefundRequestMapper.list(offset, s, blankToNull(type), blankToNull(status));
        long total = adminRefundRequestMapper.count(blankToNull(type), blankToNull(status));
        return PageResult.<RefundRequestRecord>builder()
                .records(records)
                .total(total)
                .size(s)
                .current(p)
                .build();
    }

    @Override
    public RefundRequestDetailResponse detail(Long requestId) {
        ThrowUtils.throwIf(requestId == null, ErrorCode.PARAMS_ERROR);
        RefundRequestRecord request = adminRefundRequestMapper.selectById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "退款申请不存在");

        BrokerageOrder order = request.getBrokerageOrderId() == null ? null : adminRefundMapper.selectById(request.getBrokerageOrderId());

        List<Message> chatHistory = null;
        if (request.getRoomId() != null) {
            chatHistory = adminMessageMapper.listByRoomId(request.getRoomId());
        }
        return RefundRequestDetailResponse.builder()
                .refundRequest(request)
                .order(order)
                .chatHistory(chatHistory)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long requestId, Long adminUid, String note) {
        ThrowUtils.throwIf(requestId == null || adminUid == null, ErrorCode.PARAMS_ERROR);
        RefundRequestRecord request = adminRefundRequestMapper.selectById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "退款申请不存在");
        if (!"PENDING".equals(request.getStatus())) {
            return;
        }
        ThrowUtils.throwIf(request.getBrokerageOrderId() == null, ErrorCode.OPERATION_ERROR, "退款申请缺少订单关联");
        ThrowUtils.throwIf(request.getRefundAmountFen() == null || request.getRefundAmountFen() <= 0, ErrorCode.OPERATION_ERROR, "退款金额非法");

        PaymentRefundRequest payReq = new PaymentRefundRequest();
        payReq.setContextType("BROKERAGE_ORDER");
        payReq.setContextId(request.getBrokerageOrderId());
        payReq.setRequestId(request.getId());
        payReq.setRefundAmountFen(request.getRefundAmountFen());
        String reason = request.getReason() == null || request.getReason().trim().isEmpty() ? "退款" : request.getReason().trim();
        payReq.setReason(reason);

        BaseResponse<PaymentRefundResponse> resp = paymentRefundFeignClient.refund(payReq);
        if (resp == null || resp.getCode() != 0 || resp.getData() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, resp == null ? "退款失败" : resp.getMessage());
        }
        if ("FAILED".equalsIgnoreCase(resp.getData().getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "退款失败：" + resp.getData().getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = adminRefundRequestMapper.approve(requestId, adminUid, trimTo1024(note), now);
        if (updated <= 0) {
            return;
        }
        adminRefundRequestMapper.markOrderRefunded(request.getBrokerageOrderId(), request.getRefundAmountFen());
        if (request.getCourseId() != null) {
            adminRefundRequestMapper.markCourseRefundedById(request.getCourseId());
        } else if (request.getRoomId() != null) {
            adminRefundRequestMapper.markCourseRefundedByRoomId(request.getRoomId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requestId, Long adminUid, String reason) {
        ThrowUtils.throwIf(requestId == null || adminUid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(reason == null || reason.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "拒绝原因不能为空");
        RefundRequestRecord request = adminRefundRequestMapper.selectById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "退款申请不存在");
        if (!"PENDING".equals(request.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = adminRefundRequestMapper.reject(requestId, adminUid, trimTo1024(reason), now);
        if (updated <= 0) {
            return;
        }
        if (request.getBrokerageOrderId() != null) {
            adminRefundRequestMapper.rollbackOrderPaid(request.getBrokerageOrderId());
        }
        if (request.getCourseId() != null) {
            adminRefundRequestMapper.rollbackCourseCommunicatingById(request.getCourseId());
        } else if (request.getRoomId() != null) {
            adminRefundRequestMapper.rollbackCourseCommunicatingByRoomId(request.getRoomId());
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String trimTo1024(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= 1024) return v;
        return v.substring(0, 1024);
    }
}

