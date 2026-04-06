ALTER TABLE `brokerage_order`
  ADD COLUMN `refund_locked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否进入退款流程（0否 1是）' AFTER `paid_at`,
  ADD COLUMN `refunded_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '已退款金额（分），支持部分退款追溯' AFTER `refund_locked`;

CREATE INDEX `idx_brokerage_order_refund_locked` ON `brokerage_order` (`refund_locked`);
