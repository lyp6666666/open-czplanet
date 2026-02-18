package com.ai.tutor.appointment.enums;

import lombok.Getter;

@Getter
public enum VerificationTypeEnum {
    REALNAME("REALNAME"),
    EDU("EDU");

    private final String code;

    VerificationTypeEnum(String code) {
        this.code = code;
    }

    public static VerificationTypeEnum fromCode(String code) {
        for (VerificationTypeEnum t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("unknown type: " + code);
    }
}

