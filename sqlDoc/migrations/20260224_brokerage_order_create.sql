DROP TABLE IF EXISTS `brokerage_order`;
CREATE TABLE `brokerage_order` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '中介费订单id',
  `proposal_id` bigint(20) UNSIGNED NOT NULL COMMENT '合作提案id',
  `room_id` bigint(20) UNSIGNED NOT NULL COMMENT '会话id',
  `payer_uid` bigint(20) UNSIGNED NOT NULL COMMENT '付款人uid（教师）',
  `amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '中介费金额（分）',
  `pay_method` varchar(32) DEFAULT NULL COMMENT '支付方式 WECHAT/ALIPAY',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态 PENDING/PROOF_SUBMITTED/PAID/REJECTED/CANCELED',
  `proof_url` varchar(1024) DEFAULT NULL COMMENT '支付凭证URL',
  `proof_note` varchar(512) DEFAULT NULL COMMENT '支付备注',
  `paid_at` datetime(3) DEFAULT NULL COMMENT '确认到账时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_proposal_id` (`proposal_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_payer_uid` (`payer_uid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中介费订单表';
