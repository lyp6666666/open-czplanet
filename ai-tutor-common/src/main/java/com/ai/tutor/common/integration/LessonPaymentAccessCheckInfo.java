package com.ai.tutor.common.integration;

import lombok.Data;

@Data
public class LessonPaymentAccessCheckInfo {
    /**
     * 当前准备进入/预约的课节 id。
     */
    private Long lessonId;

    /**
     * 所属长期课程 id。
     */
    private Long courseId;

    /**
     * 是否存在未支付的上一节课。
     */
    private Boolean blocked;

    /**
     * 阻塞中的上一节课支付单 id。
     */
    private Long blockingOrderId;

    /**
     * 阻塞中的上一节课课节 id。
     */
    private Long blockingLessonId;

    /**
     * 阻塞原因文案。
     */
    private String reason;
}
