package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleAvailabilityVO;
import com.ai.tutor.appointment.model.vo.schedule.ScheduleEventVO;
import com.ai.tutor.appointment.service.ScheduleService;
import com.ai.tutor.common.service.dto.RequestInfo;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 课程安排接口的 HTTP 级别测试（MockMvc）。
 *
 * <p>目标：模拟前端发起请求，校验路由路径、入参与返回结构符合约定。</p>
 */
@SpringBootTest(classes = ScheduleControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        RequestInfo info = new RequestInfo();
        info.setUid(1001L);
        RequestHolder.set(info);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void listEventsShouldMatchPathAndReturnData() throws Exception {
        ScheduleEventVO item = ScheduleEventVO.builder()
                .id(1L)
                .courseId(66L)
                .title("测试课")
                .description("备注")
                .startAt(1_771_412_400_000L)
                .endAt(1_771_416_000_000L)
                .status("RESCHEDULE_PENDING")
                .creatorUserId(1001L)
                .durationMinutes(60)
                .proposedStartAt(1_771_498_800_000L)
                .proposedEndAt(1_771_502_400_000L)
                .proposedBy(1002L)
                .cancelBy(null)
                .participant(UserSimpleVO.builder().id(1002L).name("对方").userType(2).avatar(null).build())
                .chatRoomId(10L)
                .build();
        when(scheduleService.listEvents(eq(1001L), anyLong(), anyLong(), eq(true))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/schedule/events")
                        .param("startAt", "1771412400000")
                        .param("endAt", "1771498800000")
                        .param("includePending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(66))
                .andExpect(jsonPath("$.data[0].title").value("测试课"))
                .andExpect(jsonPath("$.data[0].status").value("RESCHEDULE_PENDING"))
                .andExpect(jsonPath("$.data[0].proposedStartAt").value(1771498800000L))
                .andExpect(jsonPath("$.data[0].proposedEndAt").value(1771502400000L))
                .andExpect(jsonPath("$.data[0].participant.id").value(1002));
    }

    @Test
    void dayAvailabilityShouldMatchPathAndReturnBusyBlocks() throws Exception {
        ScheduleAvailabilityVO vo = ScheduleAvailabilityVO.builder()
                .date("2026-04-21")
                .timezone("Asia/Shanghai")
                .myUserId(1001L)
                .otherUserId(1002L)
                .myBusyBlocks(List.of(ScheduleAvailabilityVO.BusyBlockVO.builder()
                        .eventId(1L)
                        .title("我的试课")
                        .startAt(1_771_412_400_000L)
                        .endAt(1_771_419_600_000L)
                        .status("ACCEPTED")
                        .build()))
                .otherBusyBlocks(List.of())
                .build();
        when(scheduleService.getDayAvailability(eq(1001L), eq(1002L), eq(1771412400000L))).thenReturn(vo);

        mockMvc.perform(get("/api/v1/schedule/availability/day")
                        .param("otherUid", "1002")
                        .param("dateAt", "1771412400000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.data.myBusyBlocks[0].title").value("我的试课"));
    }

    @Test
    void createShouldMatchPathAndReturnEvent() throws Exception {
        ScheduleEventVO item = ScheduleEventVO.builder()
                .id(2L)
                .courseId(66L)
                .title("初二数学")
                .startAt(1_771_412_400_000L)
                .endAt(1_771_416_000_000L)
                .status("PENDING")
                .creatorUserId(1001L)
                .participant(UserSimpleVO.builder().id(1002L).name("对方").userType(2).avatar(null).build())
                .chatRoomId(11L)
                .build();
        when(scheduleService.createEvent(any(), eq(1001L))).thenReturn(item);

        String body = "{"
                + "\"courseId\":66,"
                + "\"title\":\"初二数学\","
                + "\"participantUserId\":1002,"
                + "\"startAt\":1771412400000,"
                + "\"endAt\":1771416000000,"
                + "\"description\":\"备注\""
                + "}";

        mockMvc.perform(post("/api/v1/schedule/events")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.courseId").value(66))
                .andExpect(jsonPath("$.data.chatRoomId").value(11));
    }

    @Test
    void respondShouldMatchPathAndReturnEvent() throws Exception {
        ScheduleEventVO item = ScheduleEventVO.builder()
                .id(3L)
                .title("英语")
                .startAt(1_771_412_400_000L)
                .endAt(1_771_416_000_000L)
                .status("ACCEPTED")
                .creatorUserId(1001L)
                .chatRoomId(12L)
                .build();
        when(scheduleService.respond(eq(3L), eq("ACCEPT"), eq(1001L))).thenReturn(item);

        mockMvc.perform(post("/api/v1/schedule/events/3/response")
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"ACCEPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void cancelShouldMatchPathAndReturnEvent() throws Exception {
        ScheduleEventVO item = ScheduleEventVO.builder()
                .id(4L)
                .title("物理")
                .startAt(1_771_412_400_000L)
                .endAt(1_771_416_000_000L)
                .status("CANCELED")
                .creatorUserId(1001L)
                .chatRoomId(13L)
                .build();
        when(scheduleService.cancel(eq(4L), eq(1001L), any())).thenReturn(item);

        mockMvc.perform(post("/api/v1/schedule/events/4/cancel")
                        .contentType(APPLICATION_JSON)
                        .content("{\"remark\":\"临时有事\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    void listCourseEventsShouldMatchPathAndReturnData() throws Exception {
        ScheduleEventVO item = ScheduleEventVO.builder()
                .id(5L)
                .courseId(66L)
                .title("长期课程试课")
                .status("PENDING")
                .build();
        when(scheduleService.listCourseEvents(eq(66L), eq(1001L))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/schedule/courses/66/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].courseId").value(66))
                .andExpect(jsonPath("$.data[0].id").value(5));
    }

    @Test
    void submitWeeklyScheduleShouldMatchPathAndReturnData() throws Exception {
        ScheduleEventVO item = ScheduleEventVO.builder()
                .id(6L)
                .courseId(66L)
                .title("正式每周课")
                .status("ACCEPTED")
                .build();
        when(scheduleService.submitWeeklySchedule(eq(66L), any(), eq(1001L))).thenReturn(List.of(item));

        String body = "{"
                + "\"participantUserId\":1002,"
                + "\"roomId\":11,"
                + "\"title\":\"正式每周课\","
                + "\"lessonPriceFen\":20000,"
                + "\"weeks\":8,"
                + "\"slots\":[{\"dayOfWeek\":2,\"startMinute\":1140,\"endMinute\":1260}]"
                + "}";

        mockMvc.perform(post("/api/v1/schedule/courses/66/weekly-schedule")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].courseId").value(66))
                .andExpect(jsonPath("$.data[0].id").value(6));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class
    })
    @Import(ScheduleController.class)
    static class TestConfig {
    }
}
