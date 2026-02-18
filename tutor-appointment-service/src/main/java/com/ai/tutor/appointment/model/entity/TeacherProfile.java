package com.ai.tutor.appointment.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教师资料表实体类
 * 对应表：teacher_profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfile {

    /** 教师资料id */
    private Long id;

    /** 用户id（逻辑外键） */
    private Long userId;

    /** 教师真实姓名 */
    private String realName;

    /** 学历 */
    private String education;

    /** 教授科目 */
    private String subject;

    /** 教学经验（年数） */
    private Integer experienceYears;

    /** 每小时收费 */
    private BigDecimal ratePerHour;

    /** 教师简介 */
    private String introduction;

    private String defaultGreeting;

    /** 教师证书或资格证明文件链接（JSON格式） */
    private String certificateUrls;

    private Integer basicCompleted;

    private Integer realnameVerifyStatus;

    private String realnameVerifyMethod;

    private String realnameVerifyIdFrontUrl;

    private String realnameVerifyIdBackUrl;

    @JsonIgnore
    private String realnameVerifyIdnoCipher;

    private String realnameVerifyIdnoMasked;

    private String realnameVerifyRejectReason;

    private LocalDateTime realnameVerifySubmitTime;

    private LocalDateTime realnameVerifyTime;

    private Integer eduVerifyStatus;

    private String eduVerifyProofUrls;

    private String eduVerifyRejectReason;

    private LocalDateTime eduVerifySubmitTime;

    private LocalDateTime eduVerifyTime;

    /** 状态 1正常 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
