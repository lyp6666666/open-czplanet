package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.entity.PaymentOrderRecord;
import com.ai.tutor.admin.model.vo.PageResult;

import java.time.LocalDateTime;

public interface AdminPaymentRecordService {
    PageResult<PaymentOrderRecord> list(int page,
                                        int size,
                                        String orderNo,
                                        Long userId,
                                        String contextType,
                                        Long contextId,
                                        String channel,
                                        String status,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime);

    PaymentOrderRecord detail(String orderNo);
}

