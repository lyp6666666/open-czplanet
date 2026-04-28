SET @tbl := 'collaboration_proposal';

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'trial_start_at'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE collaboration_proposal ADD COLUMN trial_start_at datetime(3) DEFAULT NULL COMMENT ''试课开始时间'' AFTER frequency_per_week',
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
    AND COLUMN_NAME = 'trial_end_at'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE collaboration_proposal ADD COLUMN trial_end_at datetime(3) DEFAULT NULL COMMENT ''试课结束时间'' AFTER trial_start_at',
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
    AND COLUMN_NAME = 'remark'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE collaboration_proposal ADD COLUMN remark varchar(1024) DEFAULT NULL COMMENT ''试课合作备注'' AFTER trial_end_at',
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
    AND COLUMN_NAME = 'expire_at'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE collaboration_proposal ADD COLUMN expire_at datetime(3) DEFAULT NULL COMMENT ''提案过期时间'' AFTER remark',
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
    AND COLUMN_NAME = 'client_request_id'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE collaboration_proposal ADD COLUMN client_request_id varchar(128) DEFAULT NULL COMMENT ''客户端幂等键'' AFTER expire_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'collaboration_proposal'
    AND INDEX_NAME = 'idx_collab_room_status_expire'
);
SET @sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_collab_room_status_expire ON collaboration_proposal (room_id, status, expire_at)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'collaboration_proposal'
    AND INDEX_NAME = 'idx_collab_from_client_req'
);
SET @sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_collab_from_client_req ON collaboration_proposal (from_uid, client_request_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
