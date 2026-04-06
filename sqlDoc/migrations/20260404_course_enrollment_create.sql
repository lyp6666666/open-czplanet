CREATE TABLE IF NOT EXISTS `course_enrollment` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程视图id',
  `application_id` bigint(20) UNSIGNED NOT NULL COMMENT '申请单id（唯一）',
  `room_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '会话id',
  `proposal_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '合作提案id',
  `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
  `student_uid` bigint(20) UNSIGNED NOT NULL COMMENT '学生uid',
  `status` varchar(32) NOT NULL COMMENT '课程状态 APPLYING/WAIT_PAY/COMMUNICATING/REFUND_REVIEW/REFUNDED/TRIALING/TRIAL_REFUND_REVIEW/TEACHING/FINISHED',
  `trial_start_at` datetime(3) DEFAULT NULL COMMENT '试课开始时间',
  `trial_end_at` datetime(3) DEFAULT NULL COMMENT '试课结束时间（开始+7天）',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_course_application` (`application_id`),
  KEY `idx_course_teacher_status` (`teacher_uid`, `status`),
  KEY `idx_course_student_status` (`student_uid`, `status`),
  KEY `idx_course_room` (`room_id`),
  KEY `idx_course_proposal` (`proposal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我的课程视图表（聚合申请/支付/合作/退款/试课状态）';
