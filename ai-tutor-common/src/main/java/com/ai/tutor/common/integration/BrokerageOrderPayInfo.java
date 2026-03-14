package com.ai.tutor.common.integration;

import lombok.Data;

/**
 * 中介费订单用于支付的必要信息（只包含支付域需要的字段）。
 */
@Data
public class BrokerageOrderPayInfo {
    /**
     * 中介费订单 id
     */
    private Long orderId;

    /**
     * 付款人 uid
     */
    private Long payerUid;

    /**
     * 支付金额（单位：分）
     */
    private Long amountFen;

    /**
     * 当前业务订单状态（由业务域定义）
     */
    private String status;

    /**
     * 支付标题
     */
    private String subject;

    /**
     * 支付描述
     */
    private String body;

    /**
     * 关联申请ID（若存在）
     */
    private Long applicationId;
}
