package com.ai.tutor.appointment.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 科目树节点，用于前端展示与选择。
 */
@Data
public class SubjectTreeNodeVO {

    private Long id;

    private Long parentId;

    private String name;

    private String grade;

    private String description;

    private List<SubjectTreeNodeVO> children = new ArrayList<>();
}

