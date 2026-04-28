CREATE TABLE IF NOT EXISTS `live_whiteboard` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `live_session_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  `schedule_event_id` BIGINT NULL,
  `scene_json` MEDIUMTEXT NOT NULL,
  `scene_version` BIGINT NOT NULL DEFAULT 0,
  `updated_by_uid` BIGINT NULL,
  `finalized` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_live_whiteboard_session` (`live_session_id`),
  KEY `idx_live_whiteboard_course` (`course_id`),
  KEY `idx_live_whiteboard_schedule_event` (`schedule_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时课堂白板快照';
