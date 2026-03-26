package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.model.dto.RejectJobRequest;
import com.ai.tutor.admin.model.entity.StudentJobPosting;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminJobService;
import com.ai.tutor.admin.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTestApplication.class)
@AutoConfigureMockMvc
public class AdminJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminJobService adminJobService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testListPendingJobs() throws Exception {
        PageResult<StudentJobPosting> result = PageResult.<StudentJobPosting>builder()
                .records(Collections.emptyList())
                .total(0)
                .size(10)
                .current(1)
                .build();

        when(adminJobService.listPendingJobs(anyInt(), anyInt())).thenReturn(result);
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/admin/jobs/pending")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    public void testApproveJob() throws Exception {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/jobs/approve/1")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminJobService).approveJob(1L);
    }

    @Test
    public void testRejectJob() throws Exception {
        RejectJobRequest request = new RejectJobRequest();
        request.setId(1L);
        request.setReason("Invalid content");

        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/jobs/reject")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminJobService).rejectJob(1L, "Invalid content");
    }
}
