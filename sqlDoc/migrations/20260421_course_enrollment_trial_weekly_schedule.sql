ALTER TABLE `course_enrollment`
  ADD COLUMN IF NOT EXISTS `teaching_mode` varchar(16) DEFAULT NULL COMMENT '授课形式 ONLINE/OFFLINE' AFTER `student_uid`,
  ADD COLUMN IF NOT EXISTS `course_name` varchar(255) DEFAULT NULL COMMENT '长期课程名称' AFTER `teaching_mode`,
  ADD COLUMN IF NOT EXISTS `class_time` varchar(255) DEFAULT NULL COMMENT '课表摘要' AFTER `course_name`,
  ADD COLUMN IF NOT EXISTS `frequency_per_week` int DEFAULT NULL COMMENT '每周课次数' AFTER `class_time`,
  ADD COLUMN IF NOT EXISTS `lesson_price` varchar(64) DEFAULT NULL COMMENT '课时费展示文案' AFTER `frequency_per_week`,
  ADD COLUMN IF NOT EXISTS `weekly_schedule_deadline_at` datetime(3) DEFAULT NULL COMMENT '试课通过后正式课表提交截止时间' AFTER `trial_end_at`,
  ADD COLUMN IF NOT EXISTS `weekly_schedule_submitted_at` datetime(3) DEFAULT NULL COMMENT '正式课表提交时间' AFTER `weekly_schedule_deadline_at`,
  ADD COLUMN IF NOT EXISTS `weekly_reminder_12h_sent_at` datetime(3) DEFAULT NULL COMMENT '正式课表 12h 提醒发送时间' AFTER `weekly_schedule_submitted_at`,
  ADD COLUMN IF NOT EXISTS `weekly_reminder_6h_sent_at` datetime(3) DEFAULT NULL COMMENT '正式课表 6h 提醒发送时间' AFTER `weekly_reminder_12h_sent_at`,
  ADD COLUMN IF NOT EXISTS `weekly_reminder_1h_sent_at` datetime(3) DEFAULT NULL COMMENT '正式课表 1h 提醒发送时间' AFTER `weekly_reminder_6h_sent_at`;

CREATE INDEX `idx_course_weekly_schedule_deadline`
  ON `course_enrollment` (`status`, `weekly_schedule_deadline_at`);
