CREATE TABLE IF NOT EXISTS `home_carousel_config` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '首页轮播图配置id',
  `title` varchar(80) NOT NULL COMMENT '主标题',
  `subtitle` varchar(160) DEFAULT NULL COMMENT '副标题',
  `image_object_key` varchar(255) NOT NULL COMMENT 'MinIO 对象 key',
  `link_type` varchar(16) DEFAULT 'NONE' COMMENT '跳转类型 NONE/ROUTE/URL',
  `link_url` varchar(255) DEFAULT NULL COMMENT '跳转地址',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `create_admin_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '创建管理员id',
  `update_admin_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '更新管理员id',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_home_carousel_sort` (`sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图配置表';
