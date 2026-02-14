package com.ai.tutor.appointment.model.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "CreateStudentJobPostingRequest", description = "家长发布需求贴请求")
public class CreateStudentJobPostingRequest {

    @NotNull
    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    @NotBlank
    @Schema(description = "标题", example = "小学三年级数学家教")
    private String title;

    @Schema(description = "描述", example = "希望讲解应用题与计算，孩子基础一般")
    private String description;

    @Schema(description = "孩子年龄", example = "9")
    private Integer childAge;

    @Schema(description = "授课方式：online/offline/both", example = "online")
    private String classMode;

    @Schema(description = "城市（线下时必填）", example = "北京")
    private String city;

    @Schema(description = "详细地址（线下可选）")
    private String address;

    @Schema(description = "预算下限（每小时）", example = "80")
    private BigDecimal budgetMin;

    @Schema(description = "预算上限（每小时）", example = "120")
    private BigDecimal budgetMax;

    @Schema(description = "期望上课时间(JSON)", example = "[\"Tue 19-21\",\"Sat 10-12\"]")
    private String schedule;
}

