package com.ai.tutor.appointment.model.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "CreateStudentJobPostingRequest", description = "家长发布需求贴请求")
public class CreateStudentJobPostingRequest {

    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    @Schema(description = "科目名称（不区分年级，可选）", example = "数学")
    @NotBlank
    private String subjectName;

    @Schema(description = "是否为其他自定义科目", example = "false")
    private Boolean subjectOther;

    @NotBlank
    @Schema(description = "标题", example = "小学三年级数学家教")
    private String title;

    @Schema(description = "描述", example = "希望讲解应用题与计算，孩子基础一般")
    @NotBlank(message = "学生情况描述不能为空")
    @Size(min = 10, message = "学生情况描述至少10个字")
    private String description;

    @Schema(description = "学员性别：male/female", example = "female")
    @NotBlank
    private String studentGender;

    @Schema(description = "学生年级编码：PRESCHOOL/GRADE1~6/JUNIOR1~3/SENIOR1~3/SELF_EXAM/COLLEGE1~4/ADULT", example = "JUNIOR1")
    private String gradeCode;

    @Schema(description = "可上课时间（自由文本）", example = "例如:每周六下午2点到4点，2周一次")
    private String availableTime;

    @Schema(description = "教师性别偏好：male/female/both", example = "both")
    private String teacherGenderPreference;

    @Schema(description = "对教员的详细要求（自由文本）", example = "对教员的学历，教学经验，性格等要求")
    @NotBlank(message = "对教员的详细要求不能为空")
    @Size(min = 10, message = "对教员的详细要求至少10个字")
    private String teacherRequirementDetail;

    @Schema(description = "孩子年龄", example = "9")
    private Integer childAge;

    @NotBlank
    @Schema(description = "授课方式：online/offline/both", example = "online")
    private String classMode;

    @Schema(description = "城市（线下时必填）", example = "北京")
    private String city;

    @Schema(description = "详细地址（线下时必填）")
    private String address;

    @NotNull
    @Min(1)
    @Max(7)
    @Schema(description = "授课频次（每周几次，1~7）", example = "2")
    private Integer frequencyPerWeek;

    @NotBlank
    @Schema(description = "发布者身份：PARENT/ STUDENT_SELF", example = "PARENT")
    private String publisherIdentity;

    @Schema(description = "预算下限（每小时）", example = "80")
    @NotNull(message = "预算不能为空")
    private BigDecimal budgetMin;

    @Schema(description = "预算上限（每小时）", example = "120")
    @NotNull(message = "预算不能为空")
    private BigDecimal budgetMax;

    @NotBlank
    @Schema(description = "授课学段：PRESCHOOL/PRIMARY/JUNIOR/SENIOR/OTHER", example = "JUNIOR")
    private String stageCode;

    @NotBlank
    @Schema(description = "学历要求：TOP2/C985/C211/DOUBLE_FIRST_CLASS/FIRST_TIER/BACHELOR/OVERSEAS/QS50/UNLIMITED", example = "C985")
    private String educationRequirement;

    @Schema(description = "期望上课时间(JSON)", example = "[\"Tue 19-21\",\"Sat 10-12\"]")
    private String schedule;
}
