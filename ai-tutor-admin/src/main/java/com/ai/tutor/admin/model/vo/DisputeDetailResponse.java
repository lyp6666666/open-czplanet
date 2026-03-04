package com.ai.tutor.admin.model.vo;

import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.entity.Message;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DisputeDetailResponse {
    private BrokerageOrder order;
    private List<Message> chatHistory;
}
