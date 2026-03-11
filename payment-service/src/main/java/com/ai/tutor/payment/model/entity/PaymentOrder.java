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
     * 支付提供方：YUNGOUOS
     */
    private String provider;

    /**
     * 订单状态：PENDING, SUCCESS, FAILED, CLOSED
     */
    private String status;

    /**
     * 第三方交易流水号
     */
    private String transactionId;

    /**
     * 第三方系统单号（如 YunGouOS orderNo）
     */
    private String providerOrderNo;

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
     * 支付要素数据（JSON：二维码图片地址/支付链接等）
     */
    private String payData;

    /**
     * 回调接收次数
     */
    private Integer notifyCount;

    /**
     * 最后一次回调接收时间
     */
    private LocalDateTime lastNotifyTime;

    /**
     * 回调验签是否通过：0否 1是
     */
    private Integer notifyVerified;

    /**
     * 支付成功事件是否已投递：0否 1是
     */
    private Integer eventSent;

    /**
     * 支付成功事件投递时间
     */
    private LocalDateTime eventSentTime;

    /**
     * 事件投递失败原因（用于排障）
     */
    private String eventSendFailReason;

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
