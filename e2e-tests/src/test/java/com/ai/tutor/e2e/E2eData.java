package com.ai.tutor.e2e;

import java.util.ArrayList;
import java.util.List;

public class E2eData {
    public Long teacherUserId;
    public Long studentUserId;
    public Long teacherProfileId;
    public Long studentProfileId;
    public Long demandId;

    public Long roomId;
    public Long applicationId;
    public Long brokerageOrderId;
    public Long refundRequestId;
    public Long courseId;

    public String teacherPhone;
    public String studentPhone;
    public String paymentOrderNo;

    public final List<Long> messageIds = new ArrayList<>();
    public final List<Long> proposalIds = new ArrayList<>();
}

