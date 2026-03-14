package com.ai.tutor.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 付款记录（管理端视角）
 *
 * <p>说明：该实体用于管理端查询 payment_order 表，字段与支付域保持一致，
 * 但仅用于查询展示，不承载支付写入逻辑。</p>
 */
@Data
@TableName("payment_order")
public class PaymentOrderRecord {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long amount;
    private String currency;
    private String channel;
    private String provider;
    private String status;
    private String transactionId;
    private String providerOrderNo;
    private Long contextId;
    private String contextType;
    private String subject;
    private String body;
    private String clientIp;
    private String extraParams;
    private String payData;
    private Integer notifyCount;
    private LocalDateTime lastNotifyTime;
    private Integer notifyVerified;
    private LocalDateTime successTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

