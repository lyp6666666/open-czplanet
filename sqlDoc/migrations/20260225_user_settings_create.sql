SET @db := DATABASE();
SET @tbl := 'user_settings';

SELECT COUNT(*) INTO @exists FROM information_schema.tables WHERE table_schema = @db AND table_name = @tbl;
SET @sql := IF(
  @exists = 0,
  'CREATE TABLE `user_settings` ('
    ' `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT ''设置id'','
    ' `user_id` bigint(20) UNSIGNED NOT NULL COMMENT ''用户id'','
    ' `application_greeting` varchar(500) DEFAULT NULL COMMENT ''默认申请问候语'','
    ' `settings_json` json DEFAULT NULL COMMENT ''扩展设置JSON'','
    ' `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),'
    ' `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),'
    ' PRIMARY KEY (`id`),'
    ' UNIQUE KEY `uniq_user_id` (`user_id`)'
  ' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''用户设置表'';',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

