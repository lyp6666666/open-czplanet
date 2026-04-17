CREATE TABLE IF NOT EXISTS `chat_realtime_event` (
  `event_id` bigint(20) UNSIGNED NOT NULL COMMENT '统一实时事件id',
  `target_uid` bigint(20) NOT NULL COMMENT '事件目标用户id',
  `event_type` varchar(64) NOT NULL COMMENT '统一事件类型',
  `biz_type` varchar(32) NOT NULL COMMENT '业务域类型',
  `room_id` bigint(20) DEFAULT NULL COMMENT '关联会话id',
  `msg_id` bigint(20) DEFAULT NULL COMMENT '关联消息id',
  `occurred_at` datetime(3) NOT NULL COMMENT '事件发生时间',
  `payload_json` longtext NOT NULL COMMENT '事件负载json',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`event_id`),
  KEY `idx_target_uid_event_id` (`target_uid`, `event_id`),
  KEY `idx_target_uid_occurred_at` (`target_uid`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM实时事件补偿表';
