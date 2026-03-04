package com.ai.tutor.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long activeTeachers;
    private Long pendingJobs;
    private Long pendingVerifications;
    private Long pendingRefunds;
}
