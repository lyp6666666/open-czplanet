package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminMessageMapper;
import com.ai.tutor.admin.mapper.AdminRefundMapper;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.entity.Message;
import com.ai.tutor.admin.model.vo.DisputeDetailResponse;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminRefundService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminRefundServiceImpl implements AdminRefundService {

    @Resource
    private AdminRefundMapper adminRefundMapper;

    @Resource
    private AdminMessageMapper adminMessageMapper;

    @Override
    public PageResult<BrokerageOrder> listRefundDisputes(int page, int size) {
        long offset = (long) (page - 1) * size;
        List<BrokerageOrder> records = adminRefundMapper.listRefundDisputes(offset, size);
        long total = adminRefundMapper.countRefundDisputes();

        return PageResult.<BrokerageOrder>builder()
                .records(records)
                .total(total)
                .size(size)
                .current(page)
                .build();
    }

    @Override
    public DisputeDetailResponse getDisputeDetails(Long orderId) {
        BrokerageOrder order = adminRefundMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR, "Order not found");

        List<Message> chatHistory = null;
        if (order.getRoomId() != null) {
            chatHistory = adminMessageMapper.listByRoomId(order.getRoomId());
        }

        return DisputeDetailResponse.builder()
                .order(order)
                .chatHistory(chatHistory)
                .build();
    }

    @Override
    public void approveRefund(Long orderId) {
        adminRefundMapper.approveRefund(orderId);
        // Trigger actual refund logic here if integrated with payment gateway
    }

    @Override
    public void rejectRefund(Long orderId, String reason) {
        // Reason could be logged or stored if schema supported it
        adminRefundMapper.rejectRefund(orderId);
    }
}
