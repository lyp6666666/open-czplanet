ALTER TABLE `course_enrollment`
  ADD COLUMN `teaching_mode` varchar(20) DEFAULT NULL COMMENT '授课形式 ONLINE/OFFLINE' AFTER `student_uid`,
  ADD COLUMN `course_name` varchar(255) DEFAULT NULL COMMENT '长期课程名称' AFTER `teaching_mode`,
  ADD COLUMN `class_time` varchar(255) DEFAULT NULL COMMENT '每周固定上课时间描述' AFTER `course_name`,
  ADD COLUMN `frequency_per_week` int DEFAULT NULL COMMENT '每周课次数' AFTER `class_time`,
  ADD COLUMN `lesson_price` varchar(64) DEFAULT NULL COMMENT '单节课价格文案' AFTER `frequency_per_week`;

ALTER TABLE `tutor_appointment`
  ADD COLUMN `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '所属长期课程 id' AFTER `id`,
  ADD KEY `idx_course_id` (`course_id`);
