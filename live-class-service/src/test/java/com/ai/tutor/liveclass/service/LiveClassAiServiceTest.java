package com.ai.tutor.liveclass.service;

import com.ai.tutor.liveclass.domain.entity.LiveClassSession;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiResultResp;
import com.ai.tutor.liveclass.integration.ai.AiAgentClient;
import com.ai.tutor.liveclass.integration.im.HttpImFacade;
import com.ai.tutor.liveclass.mapper.LiveClassSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveClassAiServiceTest {

    @Mock
    private AiAgentClient aiAgentClient;
    @Mock
    private LiveClassSessionMapper liveClassSessionMapper;
    @Mock
    private HttpImFacade httpImFacade;

    @InjectMocks
    private LiveClassAiService liveClassAiService;

    private LiveClassSession session;

    @BeforeEach
    void setUp() {
        session = LiveClassSession.builder()
                .id(8L)
                .courseId(66L)
                .teacherUid(1001L)
                .studentUid(2002L)
                .roomId(3003L)
                .providerRoomName("class-66")
                .aiPolicy("LIGHT")
                .extraJson("{}")
                .build();
    }

    @Test
    void ensureRealtimeSessionShouldSkipWhenAnotherRequestAlreadyReservedCreation() {
        when(liveClassSessionMapper.tryMarkAiSessionCreating(8L)).thenReturn(0);

        liveClassAiService.ensureRealtimeSession(session);

        verify(aiAgentClient, never()).createSession(anyLong(), any());
        verify(liveClassSessionMapper, never()).updateExtraJsonById(anyLong(), any());
    }

    @Test
    void ensureRealtimeSessionShouldPersistCreatedMarkerAfterRemoteSessionCreated() {
        when(liveClassSessionMapper.tryMarkAiSessionCreating(8L)).thenReturn(1);
        when(liveClassSessionMapper.selectById(8L)).thenReturn(LiveClassSession.builder()
                .id(8L)
                .extraJson("{\"aiSessionCreating\":true}")
                .build());
        AiAgentClient.LiveLessonSessionView created = new AiAgentClient.LiveLessonSessionView();
        created.setSessionId("lesson_ai_123");
        created.setStatus("ACTIVE");
        when(aiAgentClient.createSession(anyLong(), any())).thenReturn(created);

        liveClassAiService.ensureRealtimeSession(session);

        ArgumentCaptor<String> extraCaptor = ArgumentCaptor.forClass(String.class);
        verify(liveClassSessionMapper).updateExtraJsonById(anyLong(), extraCaptor.capture());
        String extraJson = extraCaptor.getValue();
        assertThat(extraJson).contains("\"aiSessionCreated\":true");
        assertThat(extraJson).contains("\"aiSessionId\":\"lesson_ai_123\"");
        assertThat(extraJson).contains("\"aiSessionStatus\":\"ACTIVE\"");
        assertThat(extraJson).doesNotContain("aiSessionCreating");
    }

    @Test
    void finalizeAndNotifyShouldQueueReportTaskBeforePollingResult() {
        when(liveClassSessionMapper.tryMarkAiReportTaskQueued(8L)).thenReturn(1);
        when(liveClassSessionMapper.selectById(8L)).thenReturn(LiveClassSession.builder()
                .id(8L)
                .extraJson("{\"aiSessionId\":\"lesson_ai_123\",\"aiReportTaskQueued\":true}")
                .build());
        AiAgentClient.LessonReportTaskView taskView = new AiAgentClient.LessonReportTaskView();
        taskView.setTaskId("lesson_report_123");
        taskView.setStatus("QUEUED");
        when(aiAgentClient.createLessonReportTask(anyLong(), any())).thenReturn(taskView);
        AiAgentClient.LessonReportView reportView = new AiAgentClient.LessonReportView();
        reportView.setStatus("WAITING_TEACHER_REVIEW");
        reportView.setTaskId("lesson_report_123");
        reportView.setReport(Map.of("reportTitle", "未配置未配置课后反馈", "parentSummary", "课堂总结"));
        when(aiAgentClient.getLessonReport(66L)).thenReturn(reportView);

        LiveAiResultResp result = liveClassAiService.finalizeAndNotify(session, 1001L);

        verify(aiAgentClient).finalizeLesson(66L);
        verify(aiAgentClient).createLessonReportTask(anyLong(), any());
        assertThat(result.getResultStatus()).isEqualTo("READY");
        assertThat(result.getReportStatus()).isEqualTo("WAITING_TEACHER_REVIEW");
        ArgumentCaptor<AiAgentClient.CreateLessonReportTaskRequest> requestCaptor =
                ArgumentCaptor.forClass(AiAgentClient.CreateLessonReportTaskRequest.class);
        verify(aiAgentClient).createLessonReportTask(anyLong(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLessonTopic()).isEqualTo("课程 #66 实时课堂总结");
        assertThat(requestCaptor.getValue().getExtraContext()).containsEntry("sessionId", 8L);
        assertThat(requestCaptor.getValue().getExtraContext()).containsEntry("aiSessionId", "lesson_ai_123");
    }

    @Test
    void finalizeAndNotifyShouldNotQueueDuplicateReportTaskWhenAlreadyReserved() {
        when(liveClassSessionMapper.tryMarkAiReportTaskQueued(8L)).thenReturn(0);
        AiAgentClient.LessonReportView reportView = new AiAgentClient.LessonReportView();
        reportView.setStatus("WAITING_TEACHER_REVIEW");
        reportView.setTaskId("lesson_report_existing");
        reportView.setReport(Map.of("reportTitle", "课堂反馈", "parentSummary", "课堂总结"));
        when(aiAgentClient.getLessonReport(66L)).thenReturn(reportView);

        liveClassAiService.finalizeAndNotify(session, 1001L);

        verify(aiAgentClient, never()).createLessonReportTask(anyLong(), any());
    }
}
