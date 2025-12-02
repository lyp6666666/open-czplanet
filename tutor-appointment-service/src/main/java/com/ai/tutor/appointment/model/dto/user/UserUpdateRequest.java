package com.ai.tutor.appointment.model.dto.user;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * 用户信息更新通用请求参数
 */
@Data
public class UserUpdateRequest {

    private String phone;

    /** 基础用户信息（非扩展信息，所有用户通用） */
    private BaseUserInfo baseUserInfo;

    /** 教师扩展信息（仅用户类型为1时生效） */
    private TeacherExtInfo teacherExtInfo;

    /** 学生扩展信息（仅用户类型为2时生效） */
    private StudentExtInfo studentExtInfo;
}