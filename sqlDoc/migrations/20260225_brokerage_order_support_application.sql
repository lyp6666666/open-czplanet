SET @db := DATABASE();
SET @tbl := 'brokerage_order';

SET @col := 'application_id';
SELECT COUNT(*) INTO @exists FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col;
SET @sql := IF(@exists = 0, 'ALTER TABLE brokerage_order ADD COLUMN application_id bigint(20) UNSIGNED NULL COMMENT ''申请id'' AFTER proposal_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'proposal_id';
SELECT COUNT(*) INTO @is_nullable FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col AND is_nullable = 'YES';
SET @sql := IF(@is_nullable = 0, 'ALTER TABLE brokerage_order MODIFY proposal_id bigint(20) UNSIGNED NULL COMMENT ''合作提案id''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'room_id';
SELECT COUNT(*) INTO @is_nullable FROM information_schema.columns WHERE table_schema = @db AND table_name = @tbl AND column_name = @col AND is_nullable = 'YES';
SET @sql := IF(@is_nullable = 0, 'ALTER TABLE brokerage_order MODIFY room_id bigint(20) UNSIGNED NULL COMMENT ''会话id''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'uniq_application_id';
SELECT COUNT(*) INTO @idx_exists FROM information_schema.statistics WHERE table_schema = @db AND table_name = @tbl AND index_name = @idx;
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE brokerage_order ADD UNIQUE KEY uniq_application_id (application_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

