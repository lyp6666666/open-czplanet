package com.ai.tutor.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentJobPosting {
    private Long id;
    private Long parentId;
    private Long subjectId;
    private String subjectName;
    private Integer subjectIsOther;
    private String title;
    private String description;
    private String studentGender;
    private String gradeCode;
    private String availableTime;
    private String teacherGenderPreference;
    private String teacherRequirementDetail;
    private Integer childAge;
    private String classMode;
    private String city;
    private String address;
    private Integer frequencyPerWeek;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String stageCode;
    private String educationRequirement;
    private String publisherIdentity;
    private String schedule;
    private Integer bizStatus;
    private Integer status;
    private String rejectReason;
    private LocalDateTime adminReviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
