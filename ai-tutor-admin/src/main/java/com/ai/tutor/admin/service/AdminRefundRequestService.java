package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.admin.model.vo.RefundRequestDetailResponse;

public interface AdminRefundRequestService {

    PageResult<RefundRequestRecord> list(int page, int size, String type, String status);

    RefundRequestDetailResponse detail(Long requestId);

    void approve(Long requestId, Long adminUid, String note);

    void reject(Long requestId, Long adminUid, String reason);
}

