package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.DemandViewVO;

import java.math.BigDecimal;

public interface StudentJobPostingService {

    Long create(CreateStudentJobPostingRequest request, Long uid);

    void update(Long id, UpdateStudentJobPostingRequest request, Long uid);

    StudentJobPosting getById(Long id);

    /**
     * 教师端需求详情视图（额外带发布者信息）。
     */
    DemandViewVO getViewById(Long id);

    CursorPageResponse<StudentJobPosting> listMine(CursorPageRequest request, Long uid);

    CursorPageResponse<StudentJobPosting> listPublished(Long subjectId,
                                                       String city,
                                                       String classMode,
                                                       String stageCode,
                                                       Integer frequencyPerWeek,
                                                       String educationRequirement,
                                                       String teacherGenderPreference,
                                                       BigDecimal budgetMin,
                                                       BigDecimal budgetMax,
                                                       String keyword,
                                                       String sort,
                                                       CursorPageRequest request);
}
