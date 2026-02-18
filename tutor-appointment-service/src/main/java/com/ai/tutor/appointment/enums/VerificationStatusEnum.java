package com.ai.tutor.appointment.enums;

import lombok.Getter;

@Getter
public enum VerificationStatusEnum {
    NOT_SUBMITTED(0, "未提交"),
    PENDING(1, "审核中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "未通过");

    private final int value;
    private final String desc;

    VerificationStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}

