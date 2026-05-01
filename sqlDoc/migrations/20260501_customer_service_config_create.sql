CREATE TABLE IF NOT EXISTS `customer_service_config` (
  `id` bigint(20) UNSIGNED NOT NULL COMMENT '配置id，固定为1',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用客服浮窗',
  `channel_type` varchar(32) NOT NULL DEFAULT 'WECHAT_WORK' COMMENT '微信渠道类型 WECHAT_PERSONAL/WECHAT_WORK',
  `display_name` varchar(80) NOT NULL DEFAULT '创智星球客服' COMMENT '客服展示名称',
  `wechat_no` varchar(80) DEFAULT NULL COMMENT '微信号/企业微信名称',
  `qq_no` varchar(32) DEFAULT NULL COMMENT 'QQ号',
  `qr_code_object_key` varchar(255) DEFAULT NULL COMMENT '客服二维码 MinIO 对象 key',
  `service_time` varchar(64) NOT NULL DEFAULT '09:00 - 22:00' COMMENT '服务时间',
  `description` varchar(255) DEFAULT NULL COMMENT '补充说明',
  `update_admin_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '最后更新管理员id',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工客服联系方式配置表';

INSERT INTO `customer_service_config`
(`id`, `enabled`, `channel_type`, `display_name`, `wechat_no`, `qq_no`, `qr_code_object_key`, `service_time`, `description`, `create_time`, `update_time`)
VALUES
(1, 1, 'WECHAT_WORK', '创智星球客服', 'ai_tutor_service', '123456789', NULL, '09:00 - 22:00', '添加客服时请备注：家长/老师 + 手机号', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  update_time = update_time;
