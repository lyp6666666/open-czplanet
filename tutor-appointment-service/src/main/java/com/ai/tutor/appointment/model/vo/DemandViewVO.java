package com.ai.tutor.appointment.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教师端查看需求详情的 View：
 * 1) 保持需求字段与 student_job_posting 对齐；
 * 2) 额外补齐发布者信息（头像/昵称/身份），用于 BOSS 式详情展示。
 */
@Data
@Builder
@Schema(name = "DemandViewVO", description = "需求详情视图（含发布者信息）")
public class DemandViewVO implements Serializable {

    @Schema(description = "需求ID")
    private Long id;

    @Schema(description = "发布者用户ID（家长/学生）")
    private Long parentId;

    @Schema(description = "科目ID")
    private Long subjectId;

    @Schema(description = "科目名称（不区分年级，可选）")
    private String subjectName;

    @Schema(description = "是否为其他自定义科目：1是 0否")
    private Integer subjectIsOther;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "学员性别：male/female")
    private String studentGender;

    @Schema(description = "学生年级编码：PRESCHOOL/GRADE1~6/JUNIOR1~3/SENIOR1~3/SELF_EXAM/COLLEGE1~4/ADULT")
    private String gradeCode;

    @Schema(description = "可上课时间（自由文本）")
    private String availableTime;

    @Schema(description = "教师性别偏好：male/female/both")
    private String teacherGenderPreference;

    @Schema(description = "对教员的详细要求（自由文本）")
    private String teacherRequirementDetail;

    @Schema(description = "孩子年龄")
    private Integer childAge;

    @Schema(description = "授课方式：online/offline/both")
    private String classMode;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "上课地址")
    private String address;

    @Schema(description = "授课频次（每周几次）")
    private Integer frequencyPerWeek;

    @Schema(description = "预算下限（每小时）")
    private BigDecimal budgetMin;

    @Schema(description = "预算上限（每小时）")
    private BigDecimal budgetMax;

    @Schema(description = "授课学段")
    private String stageCode;

    @Schema(description = "学历要求")
    private String educationRequirement;

    @Schema(description = "发布者身份：PARENT/ STUDENT_SELF")
    private String publisherIdentity;

    @Schema(description = "期望上课时间(JSON)")
    private String schedule;

    @Schema(description = "状态：1发布中 0关闭")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "发布者信息")
    private Publisher publisher;

    @Data
    @Builder
    @Schema(name = "DemandPublisherVO", description = "发布者信息（用于详情展示）")
    public static class Publisher implements Serializable {
        @Schema(description = "用户ID")
        private Long uid;

        @Schema(description = "展示名")
        private String displayName;

        @Schema(description = "头像")
        private String avatar;

        @Schema(description = "身份文案：学生家长/学生本人")
        private String identityLabel;
    }

    private static final long serialVersionUID = 1L;
}
