package com.ai.tutor.common.integration;

import lombok.Data;

/**
 * 线上课节支付订单的支付域最小取数字段。
 */
@Data
public class LessonPaymentPayInfo {
    /**
     * 课节支付订单 id。
     */
    private Long orderId;

    /**
     * 关联的短期课节/预约 id。
     */
    private Long lessonId;

    /**
     * 所属长期课程 id。
     */
    private Long courseId;

    /**
     * 付款学生/家长 uid。
     */
    private Long payerUid;

    /**
     * 授课教师 uid。
     */
    private Long teacherUid;

    /**
     * 本次应付金额（分）。
     */
    private Long amountFen;

    /**
     * 课节支付订单状态：PENDING/PAID/CANCELED。
     */
    private String status;

    private String subject;

    private String body;
}
