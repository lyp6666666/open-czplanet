SET @db := DATABASE();
SET @tbl := 'payment_order';

SET @col_provider := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'provider');
SET @col_provider_order_no := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'provider_order_no');
SET @col_pay_data := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'pay_data');
SET @col_notify_count := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'notify_count');
SET @col_last_notify_time := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'last_notify_time');
SET @col_notify_verified := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'notify_verified');
SET @idx_status_create_time := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = @tbl AND index_name = 'idx_status_create_time');

SET @sql := 'ALTER TABLE `payment_order` ';

SET @sql := IF(@col_provider = 0, CONCAT(@sql, ' ADD COLUMN `provider` varchar(32) NOT NULL DEFAULT ''YUNGOUOS'' COMMENT ''支付提供方：YUNGOUOS'' AFTER `channel`,'), @sql);
SET @sql := IF(@col_provider_order_no = 0, CONCAT(@sql, ' ADD COLUMN `provider_order_no` varchar(64) DEFAULT NULL COMMENT ''第三方系统单号（如 YunGouOS orderNo）'' AFTER `transaction_id`,'), @sql);
SET @sql := IF(@col_pay_data = 0, CONCAT(@sql, ' ADD COLUMN `pay_data` text COMMENT ''支付要素数据（JSON：二维码图片地址/支付链接等）'' AFTER `extra_params`,'), @sql);
SET @sql := IF(@col_notify_count = 0, CONCAT(@sql, ' ADD COLUMN `notify_count` int(11) NOT NULL DEFAULT 0 COMMENT ''回调接收次数'' AFTER `pay_data`,'), @sql);
SET @sql := IF(@col_last_notify_time = 0, CONCAT(@sql, ' ADD COLUMN `last_notify_time` datetime(3) DEFAULT NULL COMMENT ''最后一次回调接收时间'' AFTER `notify_count`,'), @sql);
SET @sql := IF(@col_notify_verified = 0, CONCAT(@sql, ' ADD COLUMN `notify_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''回调验签是否通过：0否 1是'' AFTER `last_notify_time`,'), @sql);
SET @sql := IF(@idx_status_create_time = 0, CONCAT(@sql, ' ADD KEY `idx_status_create_time` (`status`, `create_time`),'), @sql);

SET @sql := TRIM(TRAILING ',' FROM @sql);

SET @need_alter := (
  (@col_provider = 0) OR
  (@col_provider_order_no = 0) OR
  (@col_pay_data = 0) OR
  (@col_notify_count = 0) OR
  (@col_last_notify_time = 0) OR
  (@col_notify_verified = 0) OR
  (@idx_status_create_time = 0)
);

SET @final_sql := IF(@need_alter, @sql, 'SELECT "Table payment_order already extended" AS status');

PREPARE stmt FROM @final_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
