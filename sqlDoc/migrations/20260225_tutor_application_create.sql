SET @db := DATABASE();

SET @tbl := 'tutor_application';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `tutor_application` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''申请id'','
    ' `sender_uid` bigint(20) NOT NULL COMMENT ''发起方用户id'','
    ' `receiver_uid` bigint(20) NOT NULL COMMENT ''接收方用户id'','
    ' `sender_role` varchar(16) NOT NULL COMMENT ''发起方角色'','
    ' `receiver_role` varchar(16) NOT NULL COMMENT ''接收方角色'','
    ' `context_type` varchar(16) NOT NULL COMMENT ''上下文类型'','
    ' `context_id` bigint(20) NOT NULL COMMENT ''上下文id'','
    ' `content` varchar(500) NOT NULL COMMENT ''申请内容'','
    ' `client_request_id` varchar(64) DEFAULT NULL COMMENT ''幂等键'','
    ' `status` varchar(16) NOT NULL COMMENT ''状态'','
    ' `chat_access_status` varchar(32) NOT NULL COMMENT ''聊天准入状态'','
    ' `room_id` bigint(20) DEFAULT NULL COMMENT ''关联roomId'','
    ' `decided_at` datetime(3) DEFAULT NULL COMMENT ''处理时间'','
    ' `receiver_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''接收方是否已读'','
    ' `receiver_read_time` datetime(3) DEFAULT NULL COMMENT ''接收方已读时间'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_sender_client_req` (`sender_uid`, `client_request_id`),'
    ' KEY `idx_sender` (`sender_uid`),'
    ' KEY `idx_receiver` (`receiver_uid`),'
    ' KEY `idx_context` (`context_type`, `context_id`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''找家教申请表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := 'application_brokerage_order';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `application_brokerage_order` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''关联id'','
    ' `application_id` bigint(20) NOT NULL COMMENT ''申请id'','
    ' `order_id` bigint(20) NOT NULL COMMENT ''中介费订单id'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_application` (`application_id`),'
    ' KEY `idx_order` (`order_id`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''申请与中介费订单关联表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

