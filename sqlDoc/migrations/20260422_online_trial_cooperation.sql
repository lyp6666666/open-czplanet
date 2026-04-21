ALTER TABLE collaboration_proposal
    ADD COLUMN trial_start_at datetime(3) DEFAULT NULL COMMENT '试课开始时间' AFTER frequency_per_week,
    ADD COLUMN trial_end_at datetime(3) DEFAULT NULL COMMENT '试课结束时间' AFTER trial_start_at,
    ADD COLUMN remark varchar(1024) DEFAULT NULL COMMENT '试课合作备注' AFTER trial_end_at,
    ADD COLUMN expire_at datetime(3) DEFAULT NULL COMMENT '提案过期时间' AFTER remark,
    ADD COLUMN client_request_id varchar(128) DEFAULT NULL COMMENT '客户端幂等键' AFTER expire_at;

CREATE INDEX idx_collab_room_status_expire ON collaboration_proposal (room_id, status, expire_at);
CREATE INDEX idx_collab_from_client_req ON collaboration_proposal (from_uid, client_request_id);
