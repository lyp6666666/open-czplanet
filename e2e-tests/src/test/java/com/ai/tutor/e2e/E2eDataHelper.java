package com.ai.tutor.e2e;

import java.sql.SQLException;

public class E2eDataHelper {

    public static void prepareBaseData(Db db, E2eData d) throws SQLException {
        String suffix = String.valueOf(System.currentTimeMillis());
        d.teacherPhone = "1999900" + suffix.substring(Math.max(0, suffix.length() - 6));
        d.studentPhone = "1888800" + suffix.substring(Math.max(0, suffix.length() - 6));

        long teacherUserId = db.insertAndReturnId(
                "INSERT INTO user (name, phone, user_type, status, active_status) VALUES (?,?,?,?,?)",
                "e2e_teacher", d.teacherPhone, 1, 0, 2
        );
        long studentUserId = db.insertAndReturnId(
                "INSERT INTO user (name, phone, user_type, status, active_status) VALUES (?,?,?,?,?)",
                "e2e_parent", d.studentPhone, 2, 0, 2
        );
        d.teacherUserId = teacherUserId;
        d.studentUserId = studentUserId;

        d.teacherProfileId = db.insertAndReturnId(
                "INSERT INTO teacher_profile (user_id, real_name, basic_completed, resume_completed, status) VALUES (?,?,?,?,?)",
                teacherUserId, "E2E 老师", 1, 1, 1
        );
        d.studentProfileId = db.insertAndReturnId(
                "INSERT INTO student_profile (user_id, real_name, status) VALUES (?,?,?)",
                studentUserId, "E2E 家长", 1
        );

        d.demandId = db.insertAndReturnId(
                "INSERT INTO student_job_posting (parent_id, subject_name, title, student_gender, frequency_per_week, publisher_identity, biz_status, status) VALUES (?,?,?,?,?,?,?,?)",
                studentUserId, "数学", "E2E 需求标题", "male", 2, "PARENT", 1, 1
        );
    }

    public static void cleanupByIds(Db db, E2eData d) throws SQLException {
        if (d.roomId != null) {
            db.update("DELETE FROM message WHERE room_id = ?", d.roomId);
        }
        if (d.roomId != null) {
            db.update("DELETE FROM collaboration_proposal WHERE room_id = ?", d.roomId);
        }
        if (d.applicationId != null) {
            db.update("DELETE FROM tutor_application WHERE id = ?", d.applicationId);
        }
        if (d.brokerageOrderId != null) {
            db.update("DELETE FROM application_brokerage_order WHERE order_id = ?", d.brokerageOrderId);
        }
        if (d.refundRequestId != null) {
            db.update("DELETE FROM refund_request WHERE id = ?", d.refundRequestId);
        }
        if (d.courseId != null) {
            db.update("DELETE FROM course_enrollment WHERE id = ?", d.courseId);
        } else if (d.applicationId != null) {
            db.update("DELETE FROM course_enrollment WHERE application_id = ?", d.applicationId);
        }
        if (d.brokerageOrderId != null) {
            db.update("DELETE FROM brokerage_order WHERE id = ?", d.brokerageOrderId);
        }
        if (d.roomId != null) {
            db.update("DELETE FROM room WHERE id = ?", d.roomId);
        }
        if (d.demandId != null) {
            db.update("DELETE FROM student_job_posting WHERE id = ?", d.demandId);
        }
        if (d.teacherProfileId != null) {
            db.update("DELETE FROM teacher_profile WHERE id = ?", d.teacherProfileId);
        }
        if (d.studentProfileId != null) {
            db.update("DELETE FROM student_profile WHERE id = ?", d.studentProfileId);
        }
        if (d.teacherUserId != null) {
            db.update("DELETE FROM user_settings WHERE user_id = ?", d.teacherUserId);
            db.update("DELETE FROM user WHERE id = ?", d.teacherUserId);
        }
        if (d.studentUserId != null) {
            db.update("DELETE FROM user_settings WHERE user_id = ?", d.studentUserId);
            db.update("DELETE FROM user WHERE id = ?", d.studentUserId);
        }

        if (d.brokerageOrderId != null) {
            db.update("DELETE FROM payment_refund WHERE payment_order_no IN (SELECT order_no FROM payment_order WHERE context_type = 'BROKERAGE_ORDER' AND context_id = ?)", d.brokerageOrderId);
            db.update("DELETE FROM payment_order WHERE context_type = 'BROKERAGE_ORDER' AND context_id = ?", d.brokerageOrderId);
        }
    }
}
