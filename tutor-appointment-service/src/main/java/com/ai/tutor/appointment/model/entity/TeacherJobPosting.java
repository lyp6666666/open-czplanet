package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 老师发布的授课服务实体类
 * 对应表：job_posting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherJobPosting {

    /** 发布ID */
    private Long id;

    /** 家教老师ID */
    private Long tutorId;

    /** 授课科目ID（position_post.id） */
    private Long subjectId;

    /** 服务标题，例如：初中数学一对一辅导 */
    private String title;

    /** 服务描述 */
    private String description;

    /** 每小时价格 */
    private BigDecimal pricePerHour;

    /** 授课方式：online/offline/both */
    private String mode;

    /** 线下授课城市 */
    private String city;

    /** 可授课时间段（JSON 字符串） */
    private String availableTime;

    /** 最大授课人数（默认为1，表示一对一） */
    private Integer maxStudents;

    /** 状态：1-上架；0-下架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}

