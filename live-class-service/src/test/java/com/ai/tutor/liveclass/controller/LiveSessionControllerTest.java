package com.ai.tutor.liveclass.controller;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.liveclass.domain.vo.response.LiveSessionResp;
import com.ai.tutor.liveclass.domain.vo.response.PrepareLiveSessionResp;
import com.ai.tutor.liveclass.service.LiveClassService;
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
