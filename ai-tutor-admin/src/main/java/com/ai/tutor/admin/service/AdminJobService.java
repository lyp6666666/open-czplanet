package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.model.entity.StudentJobPosting;

public interface AdminJobService {
    PageResult<StudentJobPosting> listPendingJobs(int page, int size);
    void approveJob(Long id);
    void rejectJob(Long id, String reason);
}
