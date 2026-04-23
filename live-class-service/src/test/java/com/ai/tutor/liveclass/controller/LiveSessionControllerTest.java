package com.ai.tutor.liveclass.controller;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.liveclass.domain.vo.response.LiveSessionResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiResultResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiStateResp;
import com.ai.tutor.liveclass.domain.vo.response.IssueJoinTokenResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveReminderItemResp;
import com.ai.tutor.liveclass.domain.vo.response.PrepareLiveSessionResp;
import com.ai.tutor.liveclass.service.LiveClassService;
import com.ai.tutor.liveclass.service.LiveKitUrlResolver;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LiveSessionControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class LiveSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LiveClassService liveClassService;
    @MockBean
    private LiveKitUrlResolver liveKitUrlResolver;

    @BeforeEach
    void setUp() {
        RequestInfo info = new RequestInfo();
        info.setUid(1001L);
        info.setRole(1);
        RequestHolder.set(info);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldReturnLiveSessionByCourse() throws Exception {
        when(liveClassService.getByCourseId(66L, 1001L)).thenReturn(LiveSessionResp.builder()
                .sessionId(8L)
                .courseId(66L)
                .status("CREATED")
                .joinableNow(false)
                .build());

        mockMvc.perform(get("/live/sessions/by-course/66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value(8))
                .andExpect(jsonPath("$.data.courseId").value(66));
    }

    @Test
    void shouldPrepareLiveSession() throws Exception {
        when(liveClassService.prepare(eq(66L), eq(1001L), any())).thenReturn(PrepareLiveSessionResp.builder()
                .sessionId(8L)
                .status("JOIN_OPEN")
                .courseTitle("实时课程")
                .peerDisplayName("王同学")
                .canJoin(true)
                .joinableNow(true)
                .deviceCheckRequired(true)
                .build());

        mockMvc.perform(post("/live/sessions/by-course/66/prepare")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientType\":\"WEB\",\"sourcePage\":\"CHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(8))
                .andExpect(jsonPath("$.data.peerDisplayName").value("王同学"));
    }

    @Test
    void shouldReturnReminderList() throws Exception {
        when(liveClassService.myReminders(1001L)).thenReturn(List.of(
                LiveReminderItemResp.builder()
                        .sessionId(8L)
                        .courseId(66L)
                        .title("课程 #66")
                        .status("JOIN_OPEN")
                        .joinableNow(true)
                        .canJoin(true)
                        .peerDisplayName("王同学")
                        .build()
        ));

        mockMvc.perform(get("/live/sessions/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].courseId").value(66))
                .andExpect(jsonPath("$.data[0].joinableNow").value(true));
    }

    @Test
    void shouldIssueJoinTokenWithResolvedPublicWsUrl() throws Exception {
        when(liveKitUrlResolver.resolvePublicWsUrl(any())).thenReturn("wss://huoyue.online/livekit");
        when(liveClassService.issueJoinToken(eq(8L), eq(1001L), any(), eq("wss://huoyue.online/livekit")))
                .thenReturn(IssueJoinTokenResp.builder()
                        .provider("LIVEKIT")
                        .serverUrl("wss://huoyue.online/livekit")
                        .roomName("class-66")
                        .participantName("王老师")
                        .participantIdentity("1001")
                        .accessToken("mock-token")
                        .expireAt(LocalDateTime.of(2026, 4, 21, 23, 0))
                        .build());

        mockMvc.perform(post("/live/sessions/8/join-token")
                        .header("Host", "huoyue.online")
                        .header("X-Forwarded-Proto", "https")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientType\":\"WEB\",\"deviceFingerprint\":\"test-device\",\"joinMode\":\"CLASSROOM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serverUrl").value("wss://huoyue.online/livekit"))
                .andExpect(jsonPath("$.data.roomName").value("class-66"));
    }

    @Test
    void shouldAckJoinedSession() throws Exception {
        when(liveClassService.joinAck(eq(8L), eq(1001L), any())).thenReturn(LiveSessionResp.builder()
                .sessionId(8L)
                .courseId(66L)
                .status("IN_PROGRESS")
                .peerJoined(false)
                .build());

        mockMvc.perform(post("/live/sessions/8/join-ack")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientType\":\"WEB\",\"joinMode\":\"CLASSROOM\",\"connectionState\":\"CONNECTED\",\"cameraEnabled\":true,\"micEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldReturnTimeline() throws Exception {
        when(liveClassService.timeline(8L, 1001L)).thenReturn(List.of(
                com.ai.tutor.liveclass.domain.vo.response.LiveTimelineItemResp.builder()
                        .eventType("SESSION_CREATED")
                        .eventSource("APP")
                        .occurredAt(LocalDateTime.of(2026, 4, 20, 18, 0))
                        .build()
        ));

        mockMvc.perform(get("/live/sessions/8/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("SESSION_CREATED"));
    }

    @Test
    void shouldReturnAiState() throws Exception {
        when(liveClassService.aiState(8L, 1001L)).thenReturn(LiveAiStateResp.builder()
                .sessionId(8L)
                .courseId(66L)
                .aiStatus("ACTIVE")
                .realtimeEnabled(true)
                .summaryStatus("ACTIVE")
                .currentTopic("一次函数图像")
                .latestStageSummary("本阶段重点讲解了一次函数图像。")
                .studentQuestions(List.of("为什么 k 越大越陡"))
                .build());

        mockMvc.perform(get("/live/sessions/8/ai/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currentTopic").value("一次函数图像"));
    }

    @Test
    void shouldReturnAiResult() throws Exception {
        when(liveClassService.aiResult(8L, 1001L)).thenReturn(LiveAiResultResp.builder()
                .sessionId(8L)
                .courseId(66L)
                .resultStatus("READY")
                .reportStatus("WAITING_TEACHER_REVIEW")
                .preview("本节课围绕一次函数图像与应用题展开。")
                .build());

        mockMvc.perform(get("/live/sessions/8/ai/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultStatus").value("READY"))
                .andExpect(jsonPath("$.data.reportStatus").value("WAITING_TEACHER_REVIEW"));
    }

    @Test
    void shouldRetryAiResult() throws Exception {
        when(liveClassService.retryAiResult(8L, 1001L)).thenReturn(LiveAiResultResp.builder()
                .sessionId(8L)
                .courseId(66L)
                .resultStatus("FINALIZING")
                .build());

        mockMvc.perform(post("/live/sessions/8/ai/result/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultStatus").value("FINALIZING"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import({LiveSessionController.class, InternalLiveSessionController.class})
    static class TestConfig {
    }
}
