package com.ai.tutor.payment.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 */
@Data
@TableName("payment_order")
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商户订单号（唯一）
     */
    private String orderNo;

    /**
     * 支付用户ID
     */
    private Long userId;

    /**
     * 支付金额（单位：分）
     */
    private Long amount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 支付渠道：ALIPAY, WECHAT
     */
    private String channel;

    /**
     * 订单状态：PENDING, SUCCESS, FAILED, CLOSED
     */
    private String status;

    /**
     * 第三方交易流水号
     */
    private String transactionId;

    /**
     * 业务上下文ID
     */
    private Long contextId;

    /**
     * 业务上下文类型
     */
    private String contextType;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 订单描述
     */
    private String body;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 附加参数（JSON格式）
     */
    private String extraParams;

    /**
     * 支付成功时间
     */
    private LocalDateTime successTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
