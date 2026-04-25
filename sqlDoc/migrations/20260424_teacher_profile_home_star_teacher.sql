SET @tbl := 'teacher_profile';

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = @tbl
    AND COLUMN_NAME = 'home_star_teacher'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE teacher_profile ADD COLUMN home_star_teacher tinyint(4) NOT NULL DEFAULT 0 COMMENT ''首页星级教师标记 0否 1是'' AFTER edu_verify_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
