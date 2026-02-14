package com.ai.tutor.appointment.model.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "UpdateStudentJobPostingRequest", description = "家长更新需求贴请求")
public class UpdateStudentJobPostingRequest {

    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    @Schema(description = "标题", example = "小学三年级数学家教")
    private String title;

    @Schema(description = "描述")
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

    @Schema(description = "期望上课时间(JSON)")
    private String schedule;

    @Schema(description = "状态：1发布中 0关闭", example = "1")
    private Integer status;
}

