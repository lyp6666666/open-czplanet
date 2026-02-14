package com.ai.tutor.appointment.model.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "UpdateTeacherJobPostingRequest", description = "老师更新服务贴请求")
public class UpdateTeacherJobPostingRequest {

    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    @Schema(description = "标题", example = "初中数学一对一辅导")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "每小时价格", example = "150")
    private BigDecimal pricePerHour;

    @Schema(description = "授课方式：online/offline/both", example = "online")
    private String mode;

    @Schema(description = "城市（线下时必填）", example = "上海")
    private String city;

    @Schema(description = "可授课时间段(JSON)")
    private String availableTime;

    @Schema(description = "最大授课人数", example = "1")
    private Integer maxStudents;

    @Schema(description = "状态：1上架 0下架", example = "1")
    private Integer status;
}

