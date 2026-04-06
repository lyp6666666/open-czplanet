package com.ai.tutor.admin.model.vo;

import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.entity.Message;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RefundRequestDetailResponse {
    private RefundRequestRecord refundRequest;
    private BrokerageOrder order;
    private List<Message> chatHistory;
}

