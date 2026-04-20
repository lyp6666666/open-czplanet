package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LiveClassFlowE2eTest {

    private final E2eEnv env = E2eEnv.load();
    private final HttpJson http = new HttpJson();
    private static final Pattern SMS_CODE_PATTERN = Pattern.compile("code: ([0-9]{4,8})");

    private E2eData data;

    @AfterEach
    void cleanup() throws Exception {
        if (data == null) return;
        try (Db db = new Db(env)) {
            E2eDataHelper.cleanupByIds(db, data);
        }
    }

    @Test
    void scheduleToAcceptToLivePrepareAndJoin_shouldWork() throws Exception {
        Assumptions.assumeTrue(env.gatewayBaseUrl != null && !env.gatewayBaseUrl.isBlank(), "E2E_GATEWAY_BASE_URL missing");

        data = new E2eData();
        try (Db db = new Db(env)) {
            E2eDataHelper.prepareBaseData(db, data);
        }

        loginUsers();
        createScheduleEvent();
        acceptScheduleEvent();
        verifyLiveSessionSynced();
        verifyReminders();
        verifyPrepare();
        issueJoinToken();
        verifyTimeline();
    }

    private void loginUsers() throws Exception {
        data.studentToken = login("STUDENT", data.studentPhone);
        data.teacherToken = login("TEACHER", data.teacherPhone);
        assertThat(data.studentToken).isNotBlank();
        assertThat(data.teacherToken).isNotBlank();
    }

    private void createScheduleEvent() throws Exception {
        long now = System.currentTimeMillis();
        long startAt = now + 3 * 60_000;
        long endAt = startAt + 60 * 60_000;

        URI create = URI.create(env.gatewayBaseUrl + "/api/v1/schedule/events");
        Map<String, Object> req = new HashMap<>();
        req.put("title", "E2E 实时课程");
        req.put("participantUserId", data.studentUserId);
        req.put("startAt", startAt);
        req.put("endAt", endAt);
        req.put("description", "E2E 约课后进入实时课堂");
        BaseResp resp = BaseResp.from(http.postJson(create, bearerHeaders(data.teacherToken), req));
        assertThat(resp.code).isEqualTo(0);
        data.scheduleEventId = resp.data.path("id").asLong();
        assertThat(data.scheduleEventId).isPositive();
        data.roomId = resp.data.path("chatRoomId").asLong();
        assertThat(data.roomId).isPositive();
    }

    private void acceptScheduleEvent() throws Exception {
        URI respond = URI.create(env.gatewayBaseUrl + "/api/v1/schedule/events/" + data.scheduleEventId + "/response");
        BaseResp resp = BaseResp.from(http.postJson(respond, bearerHeaders(data.studentToken), Map.of("action", "ACCEPT")));
        assertThat(resp.code).isEqualTo(0);
        assertThat(resp.data.path("status").asText()).isEqualTo("ACCEPTED");
    }

    private void verifyLiveSessionSynced() throws Exception {
        URI getByCourse = URI.create(env.gatewayBaseUrl + "/live/sessions/by-course/" + data.scheduleEventId);
        BaseResp teacherView = BaseResp.from(http.get(getByCourse, bearerHeaders(data.teacherToken)));
        assertThat(teacherView.code).isEqualTo(0);
        assertThat(teacherView.data.path("courseId").asLong()).isEqualTo(data.scheduleEventId);
        assertThat(teacherView.data.path("teacherUid").asLong()).isEqualTo(data.teacherUserId);
        assertThat(teacherView.data.path("studentUid").asLong()).isEqualTo(data.studentUserId);
        assertThat(teacherView.data.path("roomId").asLong()).isEqualTo(data.roomId);
        assertThat(teacherView.data.path("joinableNow").asBoolean()).isTrue();
        data.liveSessionId = teacherView.data.path("sessionId").asLong();
        assertThat(data.liveSessionId).isPositive();

        try (Db db = new Db(env)) {
            Long liveSessionId = db.queryLong("SELECT id FROM live_class_session WHERE course_id = ?", data.scheduleEventId);
            assertThat(liveSessionId).isEqualTo(data.liveSessionId);
            String liveStatus = db.queryString("SELECT status FROM live_class_session WHERE id = ?", data.liveSessionId);
            assertThat(liveStatus).isIn("CREATED", "IN_PROGRESS", "JOIN_OPEN");
        }
    }

    private void verifyReminders() throws Exception {
        URI reminders = URI.create(env.gatewayBaseUrl + "/live/sessions/reminders");
        BaseResp resp = BaseResp.from(http.get(reminders, bearerHeaders(data.teacherToken)));
        assertThat(resp.code).isEqualTo(0);
        boolean found = false;
        for (JsonNode item : resp.data) {
            if (item.path("courseId").asLong() == data.scheduleEventId) {
                found = true;
                assertThat(item.path("canJoin").asBoolean()).isTrue();
                assertThat(item.path("joinableNow").asBoolean()).isTrue();
                break;
            }
        }
        assertThat(found).isTrue();
    }

    private void verifyPrepare() throws Exception {
        URI prepare = URI.create(env.gatewayBaseUrl + "/live/sessions/by-course/" + data.scheduleEventId + "/prepare");
        BaseResp resp = BaseResp.from(http.postJson(prepare, bearerHeaders(data.teacherToken), Map.of("clientType", "WEB", "sourcePage", "CHAT")));
        assertThat(resp.code).isEqualTo(0);
        assertThat(resp.data.path("sessionId").asLong()).isEqualTo(data.liveSessionId);
        assertThat(resp.data.path("canJoin").asBoolean()).isTrue();
        assertThat(resp.data.path("joinableNow").asBoolean()).isTrue();
        assertThat(resp.data.path("deviceCheckRequired").asBoolean()).isTrue();
    }

    private void issueJoinToken() throws Exception {
        URI token = URI.create(env.gatewayBaseUrl + "/live/sessions/" + data.liveSessionId + "/join-token");
        BaseResp resp = BaseResp.from(http.postJson(token, bearerHeaders(data.teacherToken), Map.of(
                "clientType", "WEB",
                "deviceFingerprint", "e2e-web-" + Instant.now().toEpochMilli(),
                "joinMode", "CLASSROOM"
        )));
        assertThat(resp.code).isEqualTo(0);
        assertThat(resp.data.path("provider").asText()).isEqualTo("LIVEKIT");
        assertThat(resp.data.path("roomName").asText()).isNotBlank();
        assertThat(resp.data.path("accessToken").asText()).isNotBlank();
    }

    private void verifyTimeline() throws Exception {
        URI timeline = URI.create(env.gatewayBaseUrl + "/live/sessions/" + data.liveSessionId + "/timeline");
        BaseResp resp = BaseResp.from(http.get(timeline, bearerHeaders(data.teacherToken)));
        assertThat(resp.code).isEqualTo(0);
        List<String> eventTypes = new java.util.ArrayList<>();
        for (JsonNode item : resp.data) {
            eventTypes.add(item.path("eventType").asText());
        }
        assertThat(eventTypes).contains("SESSION_CREATED");
        assertThat(eventTypes).contains("JOIN_TOKEN_ISSUED");
    }

    private String login(String role, String phone) throws Exception {
        URI sendCode = URI.create(env.gatewayBaseUrl + "/user/sendcode");
        BaseResp sendCodeResp = BaseResp.from(http.postJson(sendCode, null, Map.of("phone", phone)));
        assertThat(sendCodeResp.code).isEqualTo(0);

        String code = waitForSmsCode(phone);
        URI login = URI.create(env.gatewayBaseUrl + "/user/loginOrRegister");
        BaseResp loginResp = BaseResp.from(http.postJson(login, null, Map.of(
                "phone", phone,
                "code", code,
                "userRoleEnum", role
        )));
        assertThat(loginResp.code).isEqualTo(0);
        assertThat(loginResp.data.path("id").asLong()).isPositive();
        assertThat(loginResp.data.path("token").asText()).isNotBlank();
        return loginResp.data.path("token").asText();
    }

    private String waitForSmsCode(String phone) throws Exception {
        List<Path> candidatePaths = List.of(
                Path.of(".logs", "tutor-appointment-service.log"),
                Path.of("..", ".logs", "tutor-appointment-service.log"),
                Path.of(System.getProperty("user.dir", "."), ".logs", "tutor-appointment-service.log"),
                Path.of(System.getProperty("user.dir", "."), "..", ".logs", "tutor-appointment-service.log").normalize()
        );
        for (int i = 0; i < 20; i++) {
            for (Path logPath : candidatePaths) {
                String code = extractSmsCode(logPath, phone);
                if (code != null) {
                    return code;
                }
            }
            Thread.sleep(500L);
        }
        throw new AssertionError("sms code not found in logs for phone=" + phone + ", candidates=" + candidatePaths);
    }

    private String extractSmsCode(Path logPath, String phone) throws IOException {
        if (!Files.exists(logPath)) {
            return null;
        }
        List<String> lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (!line.contains("SMS SEND SUCCESS") || !line.contains("phone: " + phone)) {
                continue;
            }
            Matcher matcher = SMS_CODE_PATTERN.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private Map<String, String> bearerHeaders(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }
}
