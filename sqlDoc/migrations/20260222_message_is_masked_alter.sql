SET @db := DATABASE();
SET @tbl := 'message';

SET @col := 'is_masked';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE message ADD COLUMN is_masked tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否命中屏蔽规则 0否 1是'' AFTER content', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
