package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.invite.InviteOverviewVO;
import com.ai.tutor.appointment.model.vo.invite.InviteReceiverAccountVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRecordVO;
import com.ai.tutor.appointment.model.vo.invite.InviteRulesVO;
import com.ai.tutor.appointment.service.InviteService;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = InviteControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class InviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InviteService inviteService;

    @Test
    void overviewShouldReturnInviteCode() throws Exception {
        when(inviteService.overview(1001L)).thenReturn(InviteOverviewVO.builder()
                .myInviteCode("ABC123")
                .totalInviteCount(2)
                .effectiveInviteCount(1)
                .totalRewardAmountFen(1300L)
                .pendingSettlementAmountFen(800L)
                .estimatedCurrentMonthAmountFen(800L)
                .teacherRewardRate(0.13D)
                .studentRewardRate(0.13D)
                .settlementDay(10)
                .receiverConfigured(true)
                .build());

        mockMvc.perform(get("/invite/overview").requestAttr("uid", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.myInviteCode").value("ABC123"))
                .andExpect(jsonPath("$.data.receiverConfigured").value(true));
    }

    @Test
    void recordsShouldReturnCursorPage() throws Exception {
        when(inviteService.records(eq(1001L), any(), eq("ACTIVE"))).thenReturn(new CursorPageResponse<>(
                null,
                true,
                List.of(InviteRecordVO.builder()
                        .inviteeUid(2001L)
                        .inviteeDisplayName("王同学")
                        .inviteePhoneMasked("138****8000")
                        .inviteeUserType(2)
                        .registeredAt("2026-04-19T10:00:00")
                        .status("ACTIVE")
                        .hasReward(false)
                        .build())
        ));

        mockMvc.perform(get("/invite/records")
                        .requestAttr("uid", "1001")
                        .param("status", "ACTIVE")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLast").value(true))
                .andExpect(jsonPath("$.data.list[0].inviteeDisplayName").value("王同学"));
    }

    @Test
    void saveReceiverAccountShouldReturnLatestValue() throws Exception {
        when(inviteService.saveReceiverAccount(eq(1001L), any())).thenReturn(InviteReceiverAccountVO.builder()
                .receiverName("张三")
                .wechatNo("wx_zhangsan")
                .phone("13800138000")
                .remark("常用")
                .configured(true)
                .build());

        mockMvc.perform(post("/invite/receiver-account")
                        .requestAttr("uid", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverName\":\"张三\",\"wechatNo\":\"wx_zhangsan\",\"phone\":\"13800138000\",\"remark\":\"常用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.wechatNo").value("wx_zhangsan"));
        verify(inviteService).saveReceiverAccount(eq(1001L), any());
    }

    @Test
    void rulesShouldReturnConfig() throws Exception {
        when(inviteService.rules()).thenReturn(InviteRulesVO.builder()
                .teacherRewardRate(0.13D)
                .studentRewardRate(0.13D)
                .settlementDay(10)
                .minSettlementAmountFen(1000L)
                .enabled(true)
                .receiverHint("请确保微信收款信息真实有效")
                .ruleTextList(List.of("邀请教师成单返利 13%"))
                .build());

        mockMvc.perform(get("/invite/rules").requestAttr("uid", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementDay").value(10))
                .andExpect(jsonPath("$.data.ruleTextList[0]").value("邀请教师成单返利 13%"));
    }

    @Test
    void internalBrokeragePaidShouldDelegateInviteService() throws Exception {
        mockMvc.perform(post("/internal/facade/invite/brokerage-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brokerageOrderId\":9001,\"proposalId\":8001,\"teacherUid\":3001,\"studentUid\":4001,\"payerUid\":3001,\"amountFen\":10000,\"payMethod\":\"WECHAT\",\"source\":\"videoCall-IM-service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        verify(inviteService, times(1)).handleBrokerageOrderPaid(any(InviteBrokeragePaidEvent.class));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import({InviteController.class, InternalInviteController.class})
    static class TestConfig {
    }
}
