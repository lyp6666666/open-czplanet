SET @db := DATABASE();
SET @tbl := 'student_job_posting';

SET @col := 'student_gender';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE student_job_posting ADD COLUMN student_gender varchar(8) NULL DEFAULT NULL COMMENT ''学员性别：male/female''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'grade_code';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE student_job_posting ADD COLUMN grade_code varchar(16) NULL DEFAULT NULL COMMENT ''学生年级编码：PRESCHOOL/GRADE1~6/JUNIOR1~3/SENIOR1~3/SELF_EXAM/COLLEGE1~4/ADULT''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'available_time';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE student_job_posting ADD COLUMN available_time varchar(2000) NULL DEFAULT NULL COMMENT ''可上课时间（自由文本）''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'teacher_gender_preference';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE student_job_posting ADD COLUMN teacher_gender_preference varchar(8) NULL DEFAULT ''both'' COMMENT ''教师性别偏好：male/female/both''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'teacher_requirement_detail';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE student_job_posting ADD COLUMN teacher_requirement_detail text NULL COMMENT ''对教员的详细要求（自由文本）''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
