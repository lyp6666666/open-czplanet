package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.AdminTestApplication;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import com.ai.tutor.admin.model.vo.AdminInviteRewardVO;
import com.ai.tutor.admin.model.vo.AdminInviteSettlementVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminInviteService;
import com.ai.tutor.admin.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTestApplication.class)
@AutoConfigureMockMvc
class AdminInviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminInviteService adminInviteService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void listRewardsShouldReturnPageResult() throws Exception {
        PageResult<AdminInviteRewardVO> result = PageResult.<AdminInviteRewardVO>builder()
                .records(Collections.emptyList())
                .total(0)
                .size(10)
                .current(1)
                .build();
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(adminInviteService.listRewards(anyInt(), anyInt(), eq(1001L), eq(null), eq("PENDING"), eq(null), eq(null))).thenReturn(result);

        mockMvc.perform(get("/api/admin/invite/rewards")
                        .header("Authorization", "Bearer mock-token")
                        .param("inviterUid", "1001")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void listSettlementsShouldReturnPageResult() throws Exception {
        PageResult<AdminInviteSettlementVO> result = PageResult.<AdminInviteSettlementVO>builder()
                .records(Collections.emptyList())
                .total(0)
                .size(10)
                .current(1)
                .build();
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(adminInviteService.listSettlements(anyInt(), anyInt(), eq(null), eq("CREATED"), eq("2026-03"))).thenReturn(result);

        mockMvc.perform(get("/api/admin/invite/settlements")
                        .header("Authorization", "Bearer mock-token")
                        .param("status", "CREATED")
                        .param("settlementMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    void markSettlementPaidShouldDelegateService() throws Exception {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/invite/settlements/99/paid")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminInviteService).markSettlementPaid(99L);
    }

    @Test
    void markSettlementFailedShouldDelegateService() throws Exception {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/invite/settlements/99/failed")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"微信号异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(adminInviteService).markSettlementFailed(99L, "微信号异常");
    }

    @Test
    void systemConfigShouldReturnConfig() throws Exception {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(adminInviteService.systemConfig()).thenReturn(AdminInviteSystemConfigVO.builder()
                .enabled(true)
                .systemInviteCode("CHUANGZHI")
                .systemInviteLink("http://localhost:5173/auth/student?inviteCode=CHUANGZHI")
                .tutorInfoFeeDiscountRate(0.5D)
                .studentRewardRate(0.13D)
                .promoTitle("创智推广专属福利")
                .promoDesc("desc")
                .build());

        mockMvc.perform(get("/api/admin/invite/system-config")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemInviteCode").value("CHUANGZHI"));
    }

    @Test
    void saveSystemConfigShouldDelegateService() throws Exception {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(adminInviteService.saveSystemConfig(org.mockito.ArgumentMatchers.any())).thenReturn(AdminInviteSystemConfigVO.builder()
                .enabled(true)
                .systemInviteCode("CHUANGZHI")
                .systemInviteLink("http://localhost:5173/auth/student?inviteCode=CHUANGZHI")
                .tutorInfoFeeDiscountRate(0.5D)
                .studentRewardRate(0.13D)
                .promoTitle("创智推广专属福利")
                .promoDesc("desc")
                .build());

        mockMvc.perform(post("/api/admin/invite/system-config")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"systemInviteCode\":\"CHUANGZHI\",\"systemInviteLink\":\"http://localhost:5173/auth/student?inviteCode=CHUANGZHI\",\"tutorInfoFeeDiscountRate\":0.5,\"studentRewardRate\":0.13,\"promoTitle\":\"创智推广专属福利\",\"promoDesc\":\"desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemInviteCode").value("CHUANGZHI"));
    }
}
