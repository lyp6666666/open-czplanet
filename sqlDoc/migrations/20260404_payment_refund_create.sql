CREATE TABLE IF NOT EXISTS `payment_refund` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款记录id',
  `refund_no` varchar(64) NOT NULL COMMENT '平台退款单号（唯一）',
  `payment_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `provider` varchar(32) NOT NULL DEFAULT 'YUNGOUOS' COMMENT '支付提供方（YUNGOUOS）',
  `provider_refund_no` varchar(64) DEFAULT NULL COMMENT '第三方退款单号',
  `refund_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '退款金额（分）',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '退款状态 PENDING/SUCCESS/FAILED',
  `request_id` bigint(20) UNSIGNED NOT NULL COMMENT '业务幂等键（refund_request.id）',
  `fail_reason` varchar(1024) DEFAULT NULL COMMENT '失败原因（排障用）',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_refund_no` (`refund_no`),
  UNIQUE KEY `uniq_request_id` (`request_id`),
  KEY `idx_payment_refund_payment_order_status` (`payment_order_no`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款表（原路退款审计与幂等）';
