package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "中介费订单信息")
public class BrokerageOrderVO {
    private Long id;
    private Long proposalId;
    private Long roomId;
    private Long payerUid;
    private Long amountFen;
    private String payMethod;
    private String status;
    private String proofUrl;
    private String proofNote;
    private LocalDateTime paidAt;
}
