SET @db := DATABASE();

SET @tbl := 'invite_code';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `invite_code` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''邀请码记录id'','
    ' `user_id` bigint(20) UNSIGNED NOT NULL COMMENT ''用户id'','
    ' `invite_code` varchar(16) NOT NULL COMMENT ''邀请码'','
    ' `status` varchar(16) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''状态 ACTIVE/FROZEN/INVALID'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_user_id` (`user_id`),'
    ' UNIQUE KEY `uniq_invite_code` (`invite_code`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''用户邀请码表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := 'invite_system_config';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `invite_system_config` ('
    ' `id` bigint(20) UNSIGNED NOT NULL COMMENT ''配置id，固定为1'','
    ' `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否启用系统邀请码'','
    ' `system_invite_code` varchar(16) NOT NULL COMMENT ''系统邀请码'','
    ' `system_invite_link` varchar(512) DEFAULT NULL COMMENT ''系统邀请链接'','
    ' `tutor_info_fee_discount_rate` decimal(10,4) NOT NULL DEFAULT 0.5000 COMMENT ''教师信息费折扣比例'','
    ' `student_reward_rate` decimal(10,4) NOT NULL DEFAULT 0.1300 COMMENT ''学生返现比例'','
    ' `promo_title` varchar(128) DEFAULT NULL COMMENT ''推广标题'','
    ' `promo_desc` varchar(512) DEFAULT NULL COMMENT ''推广说明'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_system_invite_code` (`system_invite_code`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''系统邀请码配置表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO invite_system_config
(id, enabled, system_invite_code, system_invite_link, tutor_info_fee_discount_rate, student_reward_rate, promo_title, promo_desc, create_time, update_time)
VALUES
(1, 1, 'CHUANGZHI', 'http://localhost:5173/auth/student?inviteCode=CHUANGZHI', 0.5000, 0.1300, '创智推广专属福利', '使用创智推广码注册后，教师信息费享受推广期减半，学生可按教师实付信息费获得返现。', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE update_time = update_time;

SET @tbl := 'invite_relation';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `invite_relation` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''邀请关系id'','
    ' `inviter_uid` bigint(20) UNSIGNED NOT NULL COMMENT ''邀请人uid'','
    ' `invitee_uid` bigint(20) UNSIGNED NOT NULL COMMENT ''被邀请人uid'','
    ' `invite_code` varchar(16) NOT NULL COMMENT ''填写的邀请码'','
    ' `bind_source` varchar(32) NOT NULL DEFAULT ''REGISTER'' COMMENT ''绑定来源'','
    ' `status` varchar(16) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''状态 ACTIVE/FROZEN/INVALID'','
    ' `risk_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否命中风控'','
    ' `remark` varchar(255) DEFAULT NULL COMMENT ''备注'','
    ' `bind_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_invitee_uid` (`invitee_uid`),'
    ' KEY `idx_inviter_uid` (`inviter_uid`),'
    ' KEY `idx_invite_code` (`invite_code`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''邀请关系表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := 'invite_receiver_account';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `invite_receiver_account` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''收款信息id'','
    ' `user_id` bigint(20) UNSIGNED NOT NULL COMMENT ''用户id'','
    ' `receiver_name` varchar(64) DEFAULT NULL COMMENT ''收款人姓名'','
    ' `wechat_no` varchar(64) DEFAULT NULL COMMENT ''微信号'','
    ' `phone` varchar(32) DEFAULT NULL COMMENT ''手机号'','
    ' `remark` varchar(255) DEFAULT NULL COMMENT ''备注'','
    ' `status` varchar(16) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''状态 ACTIVE/INVALID'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_user_id` (`user_id`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''邀请返利收款信息表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := 'invite_reward_record';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `invite_reward_record` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''返利记录id'','
    ' `inviter_uid` bigint(20) UNSIGNED NOT NULL COMMENT ''邀请人uid'','
    ' `invitee_uid` bigint(20) UNSIGNED NOT NULL COMMENT ''被邀请人uid'','
    ' `reward_scene` varchar(64) NOT NULL COMMENT ''返利场景'','
    ' `biz_order_type` varchar(64) NOT NULL COMMENT ''业务订单类型'','
    ' `biz_order_id` bigint(20) UNSIGNED NOT NULL COMMENT ''业务订单id'','
    ' `payment_order_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT ''支付订单id'','
    ' `base_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''返利基数（分）'','
    ' `reward_rate` decimal(10,4) NOT NULL DEFAULT 0.0000 COMMENT ''返利比例'','
    ' `reward_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''返利金额（分）'','
    ' `status` varchar(32) NOT NULL DEFAULT ''PENDING'' COMMENT ''状态 PENDING/FROZEN/SETTLEABLE/SETTLEMENT_PENDING/PAID/FAILED/REVERSED'','
    ' `freeze_reason` varchar(255) DEFAULT NULL COMMENT ''冻结原因'','
    ' `settlement_month` varchar(16) DEFAULT NULL COMMENT ''结算月份 yyyy-MM'','
    ' `config_snapshot_json` json DEFAULT NULL COMMENT ''配置快照'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_scene_biz_order` (`reward_scene`, `biz_order_type`, `biz_order_id`),'
    ' KEY `idx_inviter_uid_status` (`inviter_uid`, `status`),'
    ' KEY `idx_invitee_uid` (`invitee_uid`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''邀请返利记录表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := 'invite_settlement_order';
SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `invite_settlement_order` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''结算单id'','
    ' `user_id` bigint(20) UNSIGNED NOT NULL COMMENT ''用户id'','
    ' `settlement_month` varchar(16) NOT NULL COMMENT ''结算月份 yyyy-MM'','
    ' `total_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''结算总金额（分）'','
    ' `paid_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''实际打款金额（分）'','
    ' `status` varchar(16) NOT NULL DEFAULT ''CREATED'' COMMENT ''状态 CREATED/PAYING/PAID/FAILED/CANCELED'','
    ' `receiver_snapshot_json` json DEFAULT NULL COMMENT ''收款信息快照'','
    ' `fail_reason` varchar(255) DEFAULT NULL COMMENT ''失败原因'','
    ' `pay_time` datetime(3) DEFAULT NULL COMMENT ''打款时间'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_user_month` (`user_id`, `settlement_month`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''邀请返利结算单表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
