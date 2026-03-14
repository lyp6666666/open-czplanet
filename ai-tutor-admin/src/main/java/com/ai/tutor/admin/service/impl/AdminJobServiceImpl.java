package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminJobMapper;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminJobService;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AdminJobServiceImpl implements AdminJobService {

    @Resource
    private AdminJobMapper adminJobMapper;

    @Override
    public PageResult<StudentJobPosting> listPendingJobs(int page, int size) {
        long offset = (long) (page - 1) * size;
        List<StudentJobPosting> records = adminJobMapper.listPendingJobs(offset, size);
        long total = adminJobMapper.countPendingJobs();
        
        return PageResult.<StudentJobPosting>builder()
                .records(records)
                .total(total)
                .size(size)
                .current(page)
                .build();
    }

    @Override
    public void approveJob(Long id) {
        adminJobMapper.approveJob(id);
    }

    @Override
    public void rejectJob(Long id, String reason) {
        adminJobMapper.rejectJob(id, reason);
    }
}
