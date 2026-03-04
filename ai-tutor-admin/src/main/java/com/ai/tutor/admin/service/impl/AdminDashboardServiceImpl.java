package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminDashboardMapper;
import com.ai.tutor.admin.model.vo.DashboardStatsResponse;
import com.ai.tutor.admin.service.AdminDashboardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    @Resource
    private AdminDashboardMapper adminDashboardMapper;

    @Override
    public DashboardStatsResponse getStats() {
        return DashboardStatsResponse.builder()
                .totalUsers(adminDashboardMapper.countTotalUsers())
                .activeTeachers(adminDashboardMapper.countActiveTeachers())
                .pendingJobs(adminDashboardMapper.countPendingJobs())
                .pendingVerifications(adminDashboardMapper.countPendingVerifications())
                .pendingRefunds(adminDashboardMapper.countPendingRefunds())
                .build();
    }
}
