package com.ai.tutor.appointment.enums;

import lombok.Getter;

/**
 * 需求发布者身份枚举：
 * - PARENT：学生家长（为孩子找家教）
 * - STUDENT_SELF：学生本人（自己找家教）
 */
@Getter
public enum PublisherIdentityEnum {
    PARENT("PARENT", "学生家长"),
    STUDENT_SELF("STUDENT_SELF", "学生本人");

    private final String code;
    private final String label;

    PublisherIdentityEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static PublisherIdentityEnum fromCode(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }
        for (PublisherIdentityEnum e : values()) {
            if (e.code.equalsIgnoreCase(v)) {
                return e;
            }
        }
        return null;
    }
}

