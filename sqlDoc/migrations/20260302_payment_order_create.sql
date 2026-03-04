SET @db := DATABASE();
SET @tbl := 'payment_order';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;

SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `payment_order` (
    `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''支付订单ID'',
    `order_no` varchar(64) NOT NULL COMMENT ''商户订单号（唯一）'',
    `user_id` bigint(20) NOT NULL COMMENT ''支付用户ID'',
    `amount` bigint(20) NOT NULL COMMENT ''支付金额（单位：分）'',
    `currency` varchar(8) NOT NULL DEFAULT ''CNY'' COMMENT ''币种'',
    `channel` varchar(32) NOT NULL COMMENT ''支付渠道：ALIPAY, WECHAT'',
    `status` varchar(32) NOT NULL COMMENT ''订单状态：PENDING, SUCCESS, FAILED, CLOSED'',
    `transaction_id` varchar(64) DEFAULT NULL COMMENT ''第三方交易流水号'',
    `context_id` bigint(20) NOT NULL COMMENT ''业务上下文ID'',
    `context_type` varchar(32) NOT NULL COMMENT ''业务上下文类型'',
    `subject` varchar(256) NOT NULL COMMENT ''订单标题'',
    `body` varchar(1024) DEFAULT NULL COMMENT ''订单描述'',
    `client_ip` varchar(64) DEFAULT NULL COMMENT ''客户端IP'',
    `extra_params` text COMMENT ''附加参数（JSON格式）'',
    `success_time` datetime(3) DEFAULT NULL COMMENT ''支付成功时间'',
    `expire_time` datetime(3) DEFAULT NULL COMMENT ''订单过期时间'',
    `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''创建时间'',
    `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT ''更新时间'',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_context` (`context_id`, `context_type`),
    KEY `idx_create_time` (`create_time`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''支付订单表''',
  'SELECT "Table payment_order already exists" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
