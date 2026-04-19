SET @db := DATABASE();

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
ON DUPLICATE KEY UPDATE
  system_invite_code = VALUES(system_invite_code),
  system_invite_link = VALUES(system_invite_link),
  promo_title = VALUES(promo_title),
  promo_desc = VALUES(promo_desc),
  update_time = NOW(3);

SET @tbl := 'brokerage_order';
SET @col := 'original_amount_fen';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE brokerage_order ADD COLUMN original_amount_fen bigint(20) UNSIGNED DEFAULT NULL COMMENT ''优惠前信息费金额（分）'' AFTER amount_fen', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'discount_amount_fen';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE brokerage_order ADD COLUMN discount_amount_fen bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''优惠金额（分）'' AFTER original_amount_fen', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'promotion_type';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE brokerage_order ADD COLUMN promotion_type varchar(64) DEFAULT NULL COMMENT ''促销类型，如 SYSTEM_INVITE'' AFTER discount_amount_fen', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'promotion_snapshot_json';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE brokerage_order ADD COLUMN promotion_snapshot_json json DEFAULT NULL COMMENT ''促销配置快照'' AFTER promotion_type', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
