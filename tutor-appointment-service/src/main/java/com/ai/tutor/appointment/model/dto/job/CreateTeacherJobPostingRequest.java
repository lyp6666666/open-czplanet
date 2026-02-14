package com.ai.tutor.appointment.model.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "CreateTeacherJobPostingRequest", description = "老师发布服务贴请求")
public class CreateTeacherJobPostingRequest {

    @NotNull
    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    @NotBlank
    @Schema(description = "标题", example = "初中数学一对一辅导")
    private String title;

    @Schema(description = "描述", example = "擅长函数与几何，支持线上板书")
    private String description;

    @NotNull
    @Schema(description = "每小时价格", example = "150")
    private BigDecimal pricePerHour;

    @Schema(description = "授课方式：online/offline/both", example = "online")
    private String mode;

    @Schema(description = "城市（线下时必填）", example = "上海")
    private String city;

    @Schema(description = "可授课时间段(JSON)", example = "[\"Mon 18-20\", \"Wed 19-21\"]")
    private String availableTime;

    @Schema(description = "最大授课人数（默认1）", example = "1")
    private Integer maxStudents;
}

