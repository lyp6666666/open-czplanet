package com.ai.tutor.payment.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付退款记录（原路退款审计与幂等）
 */
@Data
@TableName("payment_refund")
public class PaymentRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台退款单号（唯一）
     */
    private String refundNo;

    /**
     * 支付订单号（payment_order.order_no）
     */
    private String paymentOrderNo;

    /**
     * 支付提供方：YUNGOUOS
     */
    private String provider;

    /**
     * 第三方退款单号
     */
    private String providerRefundNo;

    /**
     * 退款金额（分）
     */
    private Long refundAmountFen;

    /**
     * 退款状态：PENDING / SUCCESS / FAILED
     */
    private String status;

    /**
     * 业务幂等键（refund_request.id）
     */
    private Long requestId;

    /**
     * 失败原因（排障用）
     */
    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

