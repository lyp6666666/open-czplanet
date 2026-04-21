SET @schema := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'tutor_application' AND column_name = 'teaching_mode');
SET @sql := IF(@exists = 0, 'ALTER TABLE `tutor_application` ADD COLUMN `teaching_mode` varchar(20) DEFAULT NULL COMMENT ''申请锁定的授课形式：ONLINE/OFFLINE'' AFTER `context_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'course_enrollment' AND column_name = 'teaching_mode');
SET @sql := IF(@exists = 0, 'ALTER TABLE `course_enrollment` ADD COLUMN `teaching_mode` varchar(20) DEFAULT NULL COMMENT ''授课形式 ONLINE/OFFLINE'' AFTER `student_uid`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'course_enrollment' AND column_name = 'course_name');
SET @sql := IF(@exists = 0, 'ALTER TABLE `course_enrollment` ADD COLUMN `course_name` varchar(255) DEFAULT NULL COMMENT ''长期课程名称'' AFTER `teaching_mode`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'course_enrollment' AND column_name = 'class_time');
SET @sql := IF(@exists = 0, 'ALTER TABLE `course_enrollment` ADD COLUMN `class_time` varchar(255) DEFAULT NULL COMMENT ''每周固定上课时间描述'' AFTER `course_name`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'course_enrollment' AND column_name = 'frequency_per_week');
SET @sql := IF(@exists = 0, 'ALTER TABLE `course_enrollment` ADD COLUMN `frequency_per_week` int DEFAULT NULL COMMENT ''每周课次数'' AFTER `class_time`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'course_enrollment' AND column_name = 'lesson_price');
SET @sql := IF(@exists = 0, 'ALTER TABLE `course_enrollment` ADD COLUMN `lesson_price` varchar(64) DEFAULT NULL COMMENT ''单节课价格文案'' AFTER `frequency_per_week`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema AND table_name = 'tutor_appointment' AND column_name = 'course_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `tutor_appointment` ADD COLUMN `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT ''所属长期课程 id'' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema AND table_name = 'tutor_appointment' AND index_name = 'idx_course_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `tutor_appointment` ADD KEY `idx_course_id` (`course_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
