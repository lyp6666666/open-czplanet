package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.vo.DisputeDetailResponse;
import com.ai.tutor.admin.model.vo.PageResult;

public interface AdminRefundService {
    PageResult<BrokerageOrder> listRefundDisputes(int page, int size);
    DisputeDetailResponse getDisputeDetails(Long orderId);
    void approveRefund(Long orderId);
    void rejectRefund(Long orderId, String reason);
}
