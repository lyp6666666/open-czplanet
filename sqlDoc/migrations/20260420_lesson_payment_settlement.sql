SET @tbl := 'tutor_appointment';

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'lesson_type'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `tutor_appointment` ADD COLUMN `lesson_type` varchar(20) NOT NULL DEFAULT ''NORMAL'' COMMENT ''课节类型 TRIAL/NORMAL'' AFTER `title`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'lesson_price_fen'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `tutor_appointment` ADD COLUMN `lesson_price_fen` bigint(20) UNSIGNED DEFAULT NULL COMMENT ''单节标准课价（分）'' AFTER `lesson_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'trial_price_percent'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `tutor_appointment` ADD COLUMN `trial_price_percent` int(11) NOT NULL DEFAULT 50 COMMENT ''试课收费比例，默认50表示半节课'' AFTER `lesson_price_fen`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'payable_amount_fen'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `tutor_appointment` ADD COLUMN `payable_amount_fen` bigint(20) UNSIGNED DEFAULT NULL COMMENT ''当前课节应付金额（分）'' AFTER `trial_price_percent`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `lesson_payment_order` (
    `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课节支付订单id',
    `lesson_id` bigint(20) UNSIGNED NOT NULL COMMENT '短期课节/预约id',
    `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '所属长期课程id',
    `student_uid` bigint(20) UNSIGNED NOT NULL COMMENT '学生/家长uid',
    `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
    `lesson_type` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '课节类型 TRIAL/NORMAL',
    `total_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '学生支付金额（分）',
    `platform_fee_rate` int(11) NOT NULL DEFAULT 10 COMMENT '平台服务费率百分比',
    `platform_fee_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '平台服务费金额（分）',
    `teacher_income_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '教师预计到账金额（分）',
    `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PAYING/PAID/CANCELED',
    `payment_order_no` varchar(64) DEFAULT NULL COMMENT '支付域商户订单号',
    `paid_at` datetime(3) DEFAULT NULL COMMENT '支付完成时间',
    `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_lesson_payment_lesson` (`lesson_id`),
    KEY `idx_lesson_payment_course_status` (`course_id`, `status`),
    KEY `idx_lesson_payment_student_status` (`student_uid`, `status`),
    KEY `idx_lesson_payment_teacher_status` (`teacher_uid`, `status`),
    KEY `idx_lesson_payment_order_no` (`payment_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线上课节支付订单表';

CREATE TABLE IF NOT EXISTS `teacher_settlement` (
    `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师结算记录id',
    `lesson_payment_order_id` bigint(20) UNSIGNED NOT NULL COMMENT '课节支付订单id',
    `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
    `settlement_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '教师应结算金额（分）',
    `platform_fee_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '平台服务费金额（分）',
    `status` varchar(32) NOT NULL DEFAULT 'SETTLEABLE' COMMENT '状态 SETTLEABLE/PAID',
    `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_teacher_settlement_lesson_payment` (`lesson_payment_order_id`),
    KEY `idx_teacher_settlement_teacher_status` (`teacher_uid`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师课节结算快照表';

SET @tbl := 'refund_request';

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'evidence_video_url'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `refund_request` ADD COLUMN `evidence_video_url` varchar(1024) DEFAULT NULL COMMENT ''试课不通过微信聊天录屏URL'' AFTER `evidence_images_json`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'evidence_video_duration_seconds'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `refund_request` ADD COLUMN `evidence_video_duration_seconds` int(11) DEFAULT NULL COMMENT ''录屏时长（秒，最长60秒）'' AFTER `evidence_video_url`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'evidence_video_delete_status'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `refund_request` ADD COLUMN `evidence_video_delete_status` varchar(32) DEFAULT NULL COMMENT ''录屏删除状态 PENDING_DELETE/DELETED/KEEP'' AFTER `evidence_video_duration_seconds`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'evidence_video_deleted_at'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `refund_request` ADD COLUMN `evidence_video_deleted_at` datetime(3) DEFAULT NULL COMMENT ''录屏删除标记时间'' AFTER `evidence_video_delete_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
