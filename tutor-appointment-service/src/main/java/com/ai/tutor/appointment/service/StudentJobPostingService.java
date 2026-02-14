package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;

public interface StudentJobPostingService {

    Long create(CreateStudentJobPostingRequest request, Long uid);

    void update(Long id, UpdateStudentJobPostingRequest request, Long uid);

    StudentJobPosting getById(Long id);

    CursorPageResponse<StudentJobPosting> listMine(CursorPageRequest request, Long uid);

    CursorPageResponse<StudentJobPosting> listPublished(Long subjectId, String city, String classMode, CursorPageRequest request);
}
