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

    /** 学员性别：male/female */
    private String studentGender;

    /** 学生年级编码：PRESCHOOL/GRADE1~6/JUNIOR1~3/SENIOR1~3/SELF_EXAM/COLLEGE1~4/ADULT */
    private String gradeCode;

    /** 可上课时间（自由文本） */
    private String availableTime;

    /** 教师性别偏好：male/female/both */
    private String teacherGenderPreference;

    /** 对教员的详细要求（自由文本） */
    private String teacherRequirementDetail;

    /** 孩子年龄 */
    private Integer childAge;

    /** 授课方式：online/offline/both */
    private String classMode;

    /** 线下授课城市 */
    private String city;

    /** 上课具体地址 */
    private String address;

    /**
     * 授课频次（每周几次）。
     * 用于列表/详情展示与筛选（BOSS 直聘式信息密度）。
     */
    private Integer frequencyPerWeek;

    /** 预算下限（每小时） */
    private BigDecimal budgetMin;

    /** 预算上限（每小时） */
    private BigDecimal budgetMax;

    /** 授课学段：PRESCHOOL/PRIMARY/JUNIOR/SENIOR/OTHER */
    private String stageCode;

    /** 学历要求：TOP2/C985/C211/DOUBLE_FIRST_CLASS/FIRST_TIER/BACHELOR/OVERSEAS/QS50 等 */
    private String educationRequirement;

    /**
     * 发布者身份：
     * PARENT（学生家长） / STUDENT_SELF（学生本人）。
     */
    private String publisherIdentity;

    /** 期望上课时间，JSON 格式 */
    private String schedule;

    /** 状态：1发布中 0已关闭 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
