package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.model.dto.RefundAuditRequest;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.vo.DisputeDetailResponse;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminRefundService;
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
public class AdminRefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminRefundService adminRefundService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testListRefundDisputes() throws Exception {
        PageResult<BrokerageOrder> result = PageResult.<BrokerageOrder>builder()
                .records(Collections.emptyList())
                .total(0)
                .size(10)
                .current(1)
                .build();

        when(adminRefundService.listRefundDisputes(anyInt(), anyInt())).thenReturn(result);
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/admin/refund/disputes")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    public void testGetDisputeDetails() throws Exception {
        DisputeDetailResponse response = DisputeDetailResponse.builder()
                .order(new BrokerageOrder())
                .chatHistory(Collections.emptyList())
                .build();

        when(adminRefundService.getDisputeDetails(1L)).thenReturn(response);
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/admin/refund/details/1")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testApproveRefund() throws Exception {
        RefundAuditRequest request = new RefundAuditRequest();
        request.setOrderId(1L);

        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/refund/approve")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminRefundService).approveRefund(1L);
    }

    @Test
    public void testRejectRefund() throws Exception {
        RefundAuditRequest request = new RefundAuditRequest();
        request.setOrderId(1L);
        request.setReason("Invalid claim");

        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/refund/reject")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminRefundService).rejectRefund(1L, "Invalid claim");
    }
}
