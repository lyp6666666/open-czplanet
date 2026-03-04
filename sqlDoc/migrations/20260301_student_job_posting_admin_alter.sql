-- Add reject_reason column to student_job_posting
ALTER TABLE student_job_posting ADD COLUMN reject_reason varchar(255) DEFAULT NULL COMMENT '审核拒绝原因';
-- Update status comment
ALTER TABLE student_job_posting MODIFY COLUMN status tinyint(4) DEFAULT 0 COMMENT '状态：0-待审核 1-发布中 2-已拒绝 3-已关闭';
