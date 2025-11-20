package com.ai.tutor.appointment.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {

    TEACHER(1, "teacher", "教师"),
    STUDENT(2, "student", "家长");

    private final int value;     // 数据库存储值
    private final String code;   // 标识符
    private final String desc;   // 中文描述

    UserRoleEnum(int value, String code, String desc) {
        this.value = value;
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举对象
     */
    public static UserRoleEnum fromCode(String code) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getCode().equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的用户角色类型: " + code);
    }

    /**
     * 根据数据库值获取枚举对象
     */
    public static UserRoleEnum fromValue(int value) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getValue() == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的用户角色类型值: " + value);
    }
}

