SET @db := DATABASE();
SET @tbl := 'teacher_profile';

SET @col := 'default_greeting';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN default_greeting varchar(1024) NULL DEFAULT NULL COMMENT ''默认打招呼语''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'basic_completed';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN basic_completed tinyint(1) NOT NULL DEFAULT 0 COMMENT ''基础信息是否已补全 0否 1是''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_status';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_status tinyint(4) NOT NULL DEFAULT 0 COMMENT ''实名认证状态 0未提交 1审核中 2通过 3驳回''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_method';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_method varchar(20) NULL DEFAULT NULL COMMENT ''实名认证提交方式 ID_PHOTO/NAME_IDNO''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_id_front_url';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_id_front_url varchar(255) NULL DEFAULT NULL COMMENT ''身份证人像面截图''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_id_back_url';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_id_back_url varchar(255) NULL DEFAULT NULL COMMENT ''身份证国徽面截图''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_idno_cipher';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_idno_cipher varchar(512) NULL DEFAULT NULL COMMENT ''身份证号密文/Hash''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_idno_masked';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_idno_masked varchar(32) NULL DEFAULT NULL COMMENT ''身份证号脱敏展示''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_reject_reason';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_reject_reason varchar(255) NULL DEFAULT NULL COMMENT ''实名认证驳回原因''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_submit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_submit_time datetime NULL DEFAULT NULL COMMENT ''实名认证提交时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'realname_verify_time';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN realname_verify_time datetime NULL DEFAULT NULL COMMENT ''实名认证通过/驳回时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'edu_verify_status';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN edu_verify_status tinyint(4) NOT NULL DEFAULT 0 COMMENT ''学籍/学历认证状态 0未提交 1审核中 2通过 3驳回''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'edu_verify_proof_urls';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN edu_verify_proof_urls json NULL COMMENT ''学籍/学历认证材料截图（JSON数组）''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'edu_verify_reject_reason';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN edu_verify_reject_reason varchar(255) NULL DEFAULT NULL COMMENT ''学籍/学历认证驳回原因''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'edu_verify_submit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN edu_verify_submit_time datetime NULL DEFAULT NULL COMMENT ''学籍/学历认证提交时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'edu_verify_time';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN edu_verify_time datetime NULL DEFAULT NULL COMMENT ''学籍/学历认证通过/驳回时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `room_read_state` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '已读状态id',
  `room_id` bigint(20) NOT NULL COMMENT '会话id',
  `uid` bigint(20) NOT NULL COMMENT '用户id',
  `last_read_msg_id` bigint(20) DEFAULT NULL COMMENT '最后已读消息id',
  `last_read_time` datetime(3) DEFAULT NULL COMMENT '最后已读时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_room_uid` (`room_id`, `uid`),
  KEY `idx_uid` (`uid`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话已读状态表';
