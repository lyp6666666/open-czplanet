CREATE TABLE IF NOT EXISTS ai_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  task_type VARCHAR(64) NOT NULL,
  biz_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  progress INT NOT NULL DEFAULT 0,
  message VARCHAR(255) NULL,
  input_json JSON NULL,
  output_json JSON NULL,
  error_message TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ai_task_biz (biz_id),
  KEY idx_ai_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_lesson_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  lesson_id BIGINT NOT NULL UNIQUE,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  teacher_id BIGINT NULL,
  student_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  report_json JSON NULL,
  teacher_edited_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ai_lesson_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_chat_summary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL UNIQUE,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  summary_json JSON NULL,
  message_start_id BIGINT NULL,
  message_end_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_live_lesson_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  lesson_id BIGINT NOT NULL UNIQUE,
  session_id VARCHAR(64) NOT NULL UNIQUE,
  teacher_id BIGINT NULL,
  student_id BIGINT NULL,
  subject VARCHAR(64) NULL,
  grade VARCHAR(64) NULL,
  course_type VARCHAR(32) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  asr_enabled TINYINT(1) NOT NULL DEFAULT 0,
  llm_enabled TINYINT(1) NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ai_live_lesson_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_lesson_transcript (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  lesson_id BIGINT NOT NULL,
  segment_index INT NOT NULL,
  speaker VARCHAR(32) NOT NULL,
  start_ms BIGINT NOT NULL DEFAULT 0,
  end_ms BIGINT NOT NULL DEFAULT 0,
  text TEXT NOT NULL,
  is_final TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_lesson_transcript_lesson (lesson_id),
  KEY idx_ai_lesson_transcript_segment (lesson_id, segment_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_lesson_stage_summary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  lesson_id BIGINT NOT NULL,
  stage_index INT NOT NULL,
  summary_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_lesson_stage_summary_lesson (lesson_id),
  KEY idx_ai_lesson_stage_summary_stage (lesson_id, stage_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
