package com.ai.tutor.appointment.enums;

import lombok.Getter;

@Getter
public enum RealnameVerifyMethodEnum {
    ID_PHOTO("ID_PHOTO"),
    NAME_IDNO("NAME_IDNO");

    private final String code;

    RealnameVerifyMethodEnum(String code) {
        this.code = code;
    }
}

