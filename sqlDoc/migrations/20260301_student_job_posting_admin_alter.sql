SET @tbl := 'student_job_posting';

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'reject_reason'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE student_job_posting ADD COLUMN reject_reason varchar(255) DEFAULT NULL COMMENT ''审核拒绝原因''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE student_job_posting
  MODIFY COLUMN status tinyint(4) DEFAULT 0 COMMENT '状态：0-待审核 1-发布中 2-已拒绝 3-已关闭';
