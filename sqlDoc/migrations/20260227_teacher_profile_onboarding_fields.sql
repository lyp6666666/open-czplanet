SET @db := DATABASE();
SET @tbl := 'teacher_profile';

SET @col := 'city';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN city varchar(100) NULL DEFAULT NULL COMMENT ''所在城市''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'highest_edu_school';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN highest_edu_school varchar(255) NULL DEFAULT NULL COMMENT ''最高学历学校''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'teaching_mode';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN teaching_mode varchar(20) NULL DEFAULT NULL COMMENT ''支持教学方式 ONLINE/OFFLINE/BOTH''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'resume_completed';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE teacher_profile ADD COLUMN resume_completed tinyint(1) NOT NULL DEFAULT 0 COMMENT ''简历是否已补全 0否 1是''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
