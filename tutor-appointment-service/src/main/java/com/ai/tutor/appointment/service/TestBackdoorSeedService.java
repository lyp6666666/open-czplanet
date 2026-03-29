package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.config.TestBackdoorTeacherProperties;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestBackdoorSeedService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TestBackdoorTeacherProperties props;

    public void ensureSeed() {
        Long teacherUid = props.getUserId();
        Long studentUid = props.getRedirectOtherUid();
        Long teacherProfileId = props.getUserId();
        Long studentProfileId = props.getRedirectOtherUid();
        Long roomId = props.getRedirectRoomId();
        long demandId = 666600L;
        long applicationId = 666002L;
        long orderId = 666003L;
        long appOrderId = 666004L;
        long msg1 = 9000101L;
        long msg2 = 9000102L;
        long msg3 = 9000103L;

        jdbcTemplate.update(
                "INSERT INTO user (id, name, phone, avatar, sex, status, user_type, ref_id, create_time, update_time) " +
                        "VALUES (?, '后门教师', ?, 'http://127.0.0.1:9000/ai-tutor-assets/avatars/default.svg', 1, 0, 1, ?, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE user_type=VALUES(user_type), ref_id=VALUES(ref_id), status=VALUES(status), update_time=VALUES(update_time)",
                teacherUid, props.getPhone(), teacherProfileId
        );
        jdbcTemplate.update(
                "INSERT INTO user (id, name, phone, avatar, sex, status, user_type, ref_id, create_time, update_time) " +
                        "VALUES (?, '测试学生', '15500006667', 'http://127.0.0.1:9000/ai-tutor-assets/avatars/default.svg', 0, 0, 2, ?, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE user_type=VALUES(user_type), ref_id=VALUES(ref_id), status=VALUES(status), update_time=VALUES(update_time)",
                studentUid, studentProfileId
        );

        jdbcTemplate.update(
                "INSERT INTO teacher_profile (id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, highest_edu_school, teaching_mode, resume_completed, certificate_urls, status, create_time, update_time) " +
                        "VALUES (?, ?, '后门教师', '测试 本科', '初中数学', 3, 199.00, '用于联调支付权限的测试教师账号。', '北京', '测试大学', 'ONLINE', 1, '[]', 1, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE status=VALUES(status), update_time=VALUES(update_time)",
                teacherProfileId, teacherUid
        );
        jdbcTemplate.update(
                "INSERT INTO student_profile (id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time) " +
                        "VALUES (?, ?, '测试学生', 13, '北京市海淀区', '用于联调支付权限的测试学生账号。', 120.00, 1, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE status=VALUES(status), update_time=VALUES(update_time)",
                studentProfileId, studentUid
        );

        jdbcTemplate.update(
                "INSERT INTO student_job_posting (id, parent_id, subject_name, subject_is_other, title, description, student_gender, teacher_gender_preference, teacher_requirement_detail, class_mode, frequency_per_week, publisher_identity, budget_min, budget_max, stage_code, education_requirement, schedule, biz_status, status, create_time, update_time) " +
                        "VALUES (?, ?, '数学', 0, '测试需求（后门教师支付联调）', '用于联调：学生已同意申请，教师侧应展示信息费支付卡片。', 'male', 'both', '希望老师讲解清晰、按时上课。', 'online', 2, 'PARENT', 80, 120, 'PRIMARY', 'UNLIMITED', JSON_ARRAY('Tue 19-21'), 2, 1, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE biz_status=VALUES(biz_status), status=VALUES(status), update_time=VALUES(update_time)",
                demandId, studentUid
        );

        jdbcTemplate.update(
                "INSERT INTO room (id, teacher_profile_id, student_profile_id, active_time, last_msg_id, status, create_time, update_time) " +
                        "VALUES (?, ?, ?, NOW(3), ?, 1, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE active_time=VALUES(active_time), last_msg_id=VALUES(last_msg_id), status=VALUES(status), update_time=VALUES(update_time)",
                roomId, teacherProfileId, studentProfileId, msg3
        );

        jdbcTemplate.update(
                "INSERT INTO tutor_application (id, sender_uid, receiver_uid, sender_role, receiver_role, context_type, context_id, content, client_request_id, status, chat_access_status, room_id, receiver_read, decided_at, create_time, update_time) " +
                        "VALUES (?, ?, ?, 'TEACHER', 'STUDENT', 'DEMAND', ?, '您好，我看了您的需求，和我的授课方向非常匹配，我们可以进一步详细沟通吗？', 'backdoor-seed-666002', 'ACCEPTED', 'PAYMENT_REQUIRED', ?, 1, NOW(3), NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE status=VALUES(status), chat_access_status=VALUES(chat_access_status), room_id=VALUES(room_id), receiver_read=VALUES(receiver_read), decided_at=VALUES(decided_at), update_time=VALUES(update_time)",
                applicationId, teacherUid, studentUid, demandId, roomId
        );

        jdbcTemplate.update(
                "INSERT INTO brokerage_order (id, proposal_id, application_id, room_id, payer_uid, amount_fen, pay_method, status, proof_url, proof_note, paid_at, create_time, update_time) " +
                        "VALUES (?, NULL, ?, ?, ?, 19900, NULL, 'PENDING', NULL, NULL, NULL, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE application_id=VALUES(application_id), room_id=VALUES(room_id), payer_uid=VALUES(payer_uid), amount_fen=VALUES(amount_fen), status=VALUES(status), update_time=VALUES(update_time)",
                orderId, applicationId, roomId, teacherUid
        );
        jdbcTemplate.update(
                "INSERT INTO application_brokerage_order (id, application_id, order_id, create_time, update_time) " +
                        "VALUES (?, ?, ?, NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE order_id=VALUES(order_id), update_time=VALUES(update_time)",
                appOrderId, applicationId, orderId
        );

        String extra1 = "{\"bizType\":\"TUTOR_APPLICATION\",\"eventId\":666002,\"title\":\"家教申请\",\"status\":\"PENDING\",\"creatorUserId\":666888,\"contextType\":\"DEMAND\",\"contextId\":666600,\"content\":\"您好，我看了您的需求，和我的授课方向非常匹配，我们可以进一步详细沟通吗？\"}";
        String extra2 = "{\"bizType\":\"TUTOR_APPLICATION_STATUS\",\"eventId\":666002,\"title\":\"家教申请\",\"status\":\"ACCEPTED\",\"actorUserId\":666777}";
        String extra3 = "{\"bizType\":\"BROKERAGE_REQUIRED\",\"eventId\":666003,\"proposalId\":666002,\"title\":\"信息费支付\",\"status\":\"PENDING\",\"creatorUserId\":666888,\"amountFen\":19900}";

        jdbcTemplate.update(
                "INSERT INTO message (id, room_id, from_uid, to_uid, content, reply_msg_id, status, gap_count, type, extra, create_time, update_time) " +
                        "VALUES (?, ?, ?, ?, '家教申请', NULL, 0, NULL, 8, CAST(? AS JSON), NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE content=VALUES(content), status=VALUES(status), type=VALUES(type), extra=VALUES(extra), update_time=VALUES(update_time)",
                msg1, roomId, teacherUid, studentUid, extra1
        );
        jdbcTemplate.update(
                "INSERT INTO message (id, room_id, from_uid, to_uid, content, reply_msg_id, status, gap_count, type, extra, create_time, update_time) " +
                        "VALUES (?, ?, ?, ?, '家教申请：ACCEPTED', NULL, 0, NULL, 8, CAST(? AS JSON), NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE content=VALUES(content), status=VALUES(status), type=VALUES(type), extra=VALUES(extra), update_time=VALUES(update_time)",
                msg2, roomId, studentUid, teacherUid, extra2
        );
        jdbcTemplate.update(
                "INSERT INTO message (id, room_id, from_uid, to_uid, content, reply_msg_id, status, gap_count, type, extra, create_time, update_time) " +
                        "VALUES (?, ?, ?, ?, '信息费支付', NULL, 0, NULL, 8, CAST(? AS JSON), NOW(3), NOW(3)) " +
                        "ON DUPLICATE KEY UPDATE content=VALUES(content), status=VALUES(status), type=VALUES(type), extra=VALUES(extra), update_time=VALUES(update_time)",
                msg3, roomId, studentUid, teacherUid, extra3
        );
    }
}

