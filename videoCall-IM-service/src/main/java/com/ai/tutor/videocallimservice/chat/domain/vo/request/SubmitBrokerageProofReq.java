package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "提交中介费支付凭证")
public class SubmitBrokerageProofReq {
    @Schema(description = "支付方式：WECHAT/ALIPAY")
    private String payMethod;

    @Schema(description = "支付凭证URL")
    private String proofUrl;

    @Schema(description = "支付备注")
    private String proofNote;
}
