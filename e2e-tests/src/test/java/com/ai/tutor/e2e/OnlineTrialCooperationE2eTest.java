package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineTrialCooperationE2eTest {

    private final E2eEnv env = E2eEnv.load();
    private final GatewaySign sign = new GatewaySign(env.gatewaySignSecret);
    private final HttpJson http = new HttpJson();

    private E2eData data;

    @AfterEach
    void cleanup() throws Exception {
        if (data == null) return;
        try (Db db = new Db(env)) {
            E2eDataHelper.cleanupByIds(db, data);
        } catch (SQLException ex) {
            // 本地未启动 E2E 数据库时，测试主体会被 assumption 跳过；清理阶段也不应制造额外失败。
        }
    }

    @Test
    void paymentGateAndTrialProposal_shouldWorkEndToEnd() throws Exception {
        Assumptions.assumeTrue(env.gatewaySignSecret != null && env.gatewaySignSecret.length() >= 32);
        Assumptions.assumeTrue(env.brokerageAdminToken != null && !env.brokerageAdminToken.isEmpty());
        Assumptions.assumeTrue(isDbAvailable(), "E2E MySQL 未启动，跳过真实端到端链路");
        data = new E2eData();
        try (Db db = new Db(env)) {
            E2eDataHelper.prepareBaseData(db, data);
        }

        createApplicationAndAccept();

        BaseResp blockedLesson = sendLessonRequest();
        assertThat(blockedLesson.code).isNotEqualTo(0);

        BaseResp blockedProposal = createTrialProposal();
        assertThat(blockedProposal.code).isNotEqualTo(0);

        markPaidUnlockChat();

        BaseResp okProposal = createTrialProposal();
        assertThat(okProposal.code).isEqualTo(0);
        JsonNode body = okProposal.data.path("message").path("body");
        assertThat(body.path("type").asText()).isEqualTo("collaboration_proposal");
        assertThat(body.path("trialStartAt").asLong()).isGreaterThan(0L);
        assertThat(body.path("trialEndAt").asLong()).isGreaterThan(body.path("trialStartAt").asLong());
        assertThat(body.path("remark").asText()).contains("E2E");
        assertThat(body.path("expireAt").asLong()).isGreaterThan(body.path("trialEndAt").asLong());

        try (Db db = new Db(env)) {
            Long proposalId = db.queryLong("SELECT id FROM collaboration_proposal WHERE room_id = ? ORDER BY id DESC LIMIT 1", data.roomId);
            assertThat(proposalId).isNotNull();
            data.proposalIds.add(proposalId);
        }
    }

    private boolean isDbAvailable() {
        try (Db ignored = new Db(env)) {
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    private void createApplicationAndAccept() throws Exception {
        URI startChat = URI.create(env.imBaseUrl + "/chat/application/start-chat");
        Map<String, Object> req = new HashMap<>();
        req.put("receiverUid", data.studentUserId);
        req.put("contextType", "DEMAND");
        req.put("contextId", data.demandId);
        req.put("content", "E2E 申请");
        BaseResp r1 = BaseResp.from(http.postJson(startChat, sign.headers(data.teacherUserId, 1, "POST", startChat), req));
        assertThat(r1.code).isEqualTo(0);
        data.applicationId = JsonExtract.extractApplicationId(r1.data.path("message").path("body"));
        data.roomId = r1.data.path("message").path("roomId").asLong();
        assertThat(data.applicationId).isNotNull();

        URI accept = URI.create(env.imBaseUrl + "/chat/application/" + data.applicationId + "/decision-message");
        BaseResp r2 = BaseResp.from(http.postJson(accept, sign.headers(data.studentUserId, 2, "POST", accept), Map.of("action", "ACCEPT")));
        assertThat(r2.code).isEqualTo(0);

        URI detail = URI.create(env.imBaseUrl + "/chat/application/" + data.applicationId);
        BaseResp r3 = BaseResp.from(http.get(detail, sign.headers(data.teacherUserId, 1, "GET", detail)));
        assertThat(r3.code).isEqualTo(0);
        data.brokerageOrderId = r3.data.path("orderId").asLong();
        assertThat(data.brokerageOrderId).isNotNull();
    }

    private BaseResp sendLessonRequest() throws Exception {
        URI uri = URI.create(env.imBaseUrl + "/chat/msg");
        long start = Instant.now().plusSeconds(86_400).toEpochMilli();
        Map<String, Object> body = new HashMap<>();
        body.put("bizType", "LESSON_REQUEST");
        body.put("eventId", 99L);
        body.put("title", "E2E 未支付试课");
        body.put("startAt", start);
        body.put("endAt", start + 7_200_000L);
        body.put("status", "PENDING");
        body.put("creatorUserId", data.studentUserId);
        Map<String, Object> req = new HashMap<>();
        req.put("roomId", data.roomId);
        req.put("msgType", 8);
        req.put("body", body);
        return BaseResp.from(http.postJson(uri, sign.headers(data.studentUserId, 2, "POST", uri), req));
    }

    private BaseResp createTrialProposal() throws Exception {
        URI create = URI.create(env.imBaseUrl + "/chat/collaboration/proposal");
        long start = Instant.now().plusSeconds(172_800).toEpochMilli();
        Map<String, Object> req = new HashMap<>();
        req.put("roomId", data.roomId);
        req.put("pricePerHour", "200 元/小时");
        req.put("trialStartAt", start);
        req.put("trialEndAt", start + 7_200_000L);
        req.put("remark", "E2E 试课合作提案");
        req.put("clientRequestId", "e2e-trial-" + System.nanoTime());
        return BaseResp.from(http.postJson(create, sign.headers(data.studentUserId, 2, "POST", create), req));
    }

    private void markPaidUnlockChat() throws Exception {
        URI markPaid = URI.create(env.imBaseUrl + "/chat/brokerage/admin/order/" + data.brokerageOrderId + "/mark-paid");
        Map<String, String> headers = new HashMap<>(sign.headers(999999L, 0, "POST", markPaid));
        headers.put("X-Admin-Token", env.brokerageAdminToken);
        BaseResp r = BaseResp.from(http.postJson(markPaid, headers, Map.of()));
        assertThat(r.code).isEqualTo(0);
    }
}
