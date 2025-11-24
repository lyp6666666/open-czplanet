package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生（家长）发布的家教岗位需求表实体类
 * 对应表：student_job_posting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentJobPosting {

    /** 岗位需求ID */
    private Long id;

    /** 家长用户ID（逻辑外键，对应 user.id） */
    private Long parentId;

    /** 需求科目ID（逻辑外键，对应 position_post.id） */
    private Long subjectId;

    /** 岗位标题，如：小学数学辅导 */
    private String title;

    /** 岗位描述（需求详情） */
    private String description;

    /** 孩子年龄 */
    private Integer childAge;

    /** 授课方式：online/offline/both */
    private String classMode;

    /** 线下授课城市 */
    private String city;

    /** 上课具体地址 */
    private String address;

    /** 预算下限（每小时） */
    private BigDecimal budgetMin;

    /** 预算上限（每小时） */
    private BigDecimal budgetMax;

    /** 期望上课时间，JSON 格式 */
    private String schedule;

    /** 状态：1发布中 0已关闭 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}