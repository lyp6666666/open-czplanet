SET @db := DATABASE();
SET @tbl := 'payment_order';

SET @col_event_sent := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'event_sent');
SET @col_event_sent_time := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'event_sent_time');
SET @col_event_send_fail_reason := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = 'event_send_fail_reason');

SET @idx_event_sent := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = @tbl AND index_name = 'idx_event_sent');

SET @sql := 'ALTER TABLE `payment_order` ';

SET @sql := IF(@col_event_sent = 0, CONCAT(@sql, ' ADD COLUMN `event_sent` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''支付成功事件是否已投递：0否 1是'' AFTER `notify_verified`,'), @sql);
SET @sql := IF(@col_event_sent_time = 0, CONCAT(@sql, ' ADD COLUMN `event_sent_time` datetime(3) DEFAULT NULL COMMENT ''支付成功事件投递时间'' AFTER `event_sent`,'), @sql);
SET @sql := IF(@col_event_send_fail_reason = 0, CONCAT(@sql, ' ADD COLUMN `event_send_fail_reason` varchar(256) DEFAULT NULL COMMENT ''事件投递失败原因（用于排障）'' AFTER `event_sent_time`,'), @sql);
SET @sql := IF(@idx_event_sent = 0, CONCAT(@sql, ' ADD KEY `idx_event_sent` (`status`, `event_sent`, `update_time`),'), @sql);

SET @sql := TRIM(TRAILING ',' FROM @sql);

SET @need_alter := (
  (@col_event_sent = 0) OR
  (@col_event_sent_time = 0) OR
  (@col_event_send_fail_reason = 0) OR
  (@idx_event_sent = 0)
);

SET @final_sql := IF(@need_alter, @sql, 'SELECT "Table payment_order event delivery columns already exist" AS status');

PREPARE stmt FROM @final_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

