package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.model.dto.VerificationAuditRequest;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminVerificationService;
import com.ai.tutor.admin.utils.JwtUtil;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
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
public class AdminVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminVerificationService adminVerificationService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testListPendingVerifications() throws Exception {
        PageResult<TeacherProfile> result = PageResult.<TeacherProfile>builder()
                .records(Collections.emptyList())
                .total(0)
                .size(10)
                .current(1)
                .build();

        when(adminVerificationService.listPendingVerifications(anyInt(), anyInt())).thenReturn(result);
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/admin/verification/pending")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    public void testApproveVerification() throws Exception {
        VerificationAuditRequest request = new VerificationAuditRequest();
        request.setUserId(1L);
        request.setType("REALNAME");

        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/verification/approve")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminVerificationService).approveVerification(1L, "REALNAME");
    }

    @Test
    public void testRejectVerification() throws Exception {
        VerificationAuditRequest request = new VerificationAuditRequest();
        request.setUserId(1L);
        request.setType("EDU");
        request.setReason("Invalid document");

        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/verification/reject")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminVerificationService).rejectVerification(1L, "EDU", "Invalid document");
    }
}
