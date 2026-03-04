package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.model.vo.DashboardStatsResponse;
import com.ai.tutor.admin.service.AdminDashboardService;
import com.ai.tutor.admin.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTestApplication.class)
@AutoConfigureMockMvc
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    public void testGetStats() throws Exception {
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalUsers(100L)
                .activeTeachers(20L)
                .pendingJobs(5L)
                .pendingVerifications(3L)
                .pendingRefunds(1L)
                .build();

        when(adminDashboardService.getStats()).thenReturn(stats);
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.activeTeachers").value(20));
    }
}
