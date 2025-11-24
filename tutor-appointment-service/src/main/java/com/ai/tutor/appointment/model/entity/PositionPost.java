package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 家教科目分类表实体类
 * 对应表：position_post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionPost {

    /** 科目ID */
    private Long id;

    /** 父科目ID（如：小学 -> 数学） */
    private Long parentId;

    /** 科目名称，如：数学、英语、钢琴 */
    private String name;

    /** 年级段：小学/初中/高中/通用 */
    private String grade;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 状态 1启用 0禁用 */
    private Integer enableStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}