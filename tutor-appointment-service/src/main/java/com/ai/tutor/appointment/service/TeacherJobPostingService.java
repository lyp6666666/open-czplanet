package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateTeacherJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateTeacherJobPostingRequest;
import com.ai.tutor.appointment.model.entity.TeacherJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;

public interface TeacherJobPostingService {

    Long create(CreateTeacherJobPostingRequest request, Long uid);

    void update(Long id, UpdateTeacherJobPostingRequest request, Long uid);

    TeacherJobPosting getById(Long id);

    CursorPageResponse<TeacherJobPosting> listMine(CursorPageRequest request, Long uid);

    CursorPageResponse<TeacherJobPosting> listPublished(Long subjectId, String city, String mode, CursorPageRequest request);
}
