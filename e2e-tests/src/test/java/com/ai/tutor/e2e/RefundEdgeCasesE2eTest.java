package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RefundEdgeCasesE2eTest {

    private final E2eEnv env = E2eEnv.load();
    private final GatewaySign sign = new GatewaySign(env.gatewaySignSecret);
    private final HttpJson http = new HttpJson();

    private E2eData data;

    @AfterEach
    void cleanup() throws Exception {
        if (data == null) return;
        try (Db db = new Db(env)) {
            E2eDataHelper.cleanupByIds(db, data);
        }
    }

    @Test
    void refundBeforePayment_shouldFail() throws Exception {
        Assumptions.assumeTrue(env.gatewaySignSecret != null && env.gatewaySignSecret.length() >= 32);
        data = new E2eData();
        try (Db db = new Db(env)) {
            E2eDataHelper.prepareBaseData(db, data);
        }

        createApplicationAndAccept();

        URI state = URI.create(env.imBaseUrl + "/chat/refund/state?roomId=" + data.roomId);
        BaseResp st = BaseResp.from(http.get(state, sign.headers(data.teacherUserId, 1, "GET", state)));
        assertThat(st.code).isEqualTo(0);
        assertThat(st.data.path("canApply").asBoolean()).isFalse();

        URI apply = URI.create(env.imBaseUrl + "/chat/refund/apply");
        Map<String, Object> req = new HashMap<>();
        req.put("roomId", data.roomId);
        req.put("reason", "E2E");
        BaseResp r = BaseResp.from(http.postJson(apply, sign.headers(data.teacherUserId, 1, "POST", apply), req));
        assertThat(r.code).isNotEqualTo(0);
    }

    @Test
    void studentIsNotPayer_refundApplyShouldFail() throws Exception {
        Assumptions.assumeTrue(env.gatewaySignSecret != null && env.gatewaySignSecret.length() >= 32);
        Assumptions.assumeTrue(env.brokerageAdminToken != null && !env.brokerageAdminToken.isEmpty());
        data = new E2eData();
        try (Db db = new Db(env)) {
            E2eDataHelper.prepareBaseData(db, data);
        }

        createApplicationAndAccept();
        prepay();
        markPaidUnlockChat();

        URI state = URI.create(env.imBaseUrl + "/chat/refund/state?roomId=" + data.roomId);
        BaseResp st = BaseResp.from(http.get(state, sign.headers(data.studentUserId, 2, "GET", state)));
        assertThat(st.code).isEqualTo(0);
        assertThat(st.data.path("canApply").asBoolean()).isFalse();

        URI apply = URI.create(env.imBaseUrl + "/chat/refund/apply");
        Map<String, Object> req = new HashMap<>();
        req.put("roomId", data.roomId);
        req.put("reason", "E2E");
        BaseResp r = BaseResp.from(http.postJson(apply, sign.headers(data.studentUserId, 2, "POST", apply), req));
        assertThat(r.code).isNotEqualTo(0);
    }

    @Test
    void collaborationAccepted_shouldDisableChatRefund_andTrialRefundValidations() throws Exception {
        Assumptions.assumeTrue(env.gatewaySignSecret != null && env.gatewaySignSecret.length() >= 32);
        Assumptions.assumeTrue(env.brokerageAdminToken != null && !env.brokerageAdminToken.isEmpty());
        data = new E2eData();
        try (Db db = new Db(env)) {
            E2eDataHelper.prepareBaseData(db, data);
        }

        createApplicationAndAccept();
        prepay();
        markPaidUnlockChat();

        Long proposalId = createCollaborationProposal();
        acceptCollaborationProposal(proposalId);

        URI state = URI.create(env.imBaseUrl + "/chat/refund/state?roomId=" + data.roomId);
        BaseResp st = BaseResp.from(http.get(state, sign.headers(data.teacherUserId, 1, "GET", state)));
        assertThat(st.code).isEqualTo(0);
        assertThat(st.data.path("canApply").asBoolean()).isFalse();

        URI myCourses = URI.create(env.imBaseUrl + "/courses/my?page=1&size=20&role=TEACHER");
        BaseResp list = BaseResp.from(http.get(myCourses, sign.headers(data.teacherUserId, 1, "GET", myCourses)));
        assertThat(list.code).isEqualTo(0);
        JsonNode arr = list.data;
        assertThat(arr.isArray()).isTrue();
        Long courseId = arr.size() > 0 ? arr.get(0).path("courseId").asLong() : null;
        assertThat(courseId).isNotNull();
        data.courseId = courseId;

        URI trialApply = URI.create(env.imBaseUrl + "/courses/" + courseId + "/trial-refund/apply");
        BaseResp bad1 = BaseResp.from(http.postJson(trialApply, sign.headers(data.teacherUserId, 1, "POST", trialApply), Map.of("reason", "", "evidenceImageUrls", List.of("https://x/1.jpg"))));
        assertThat(bad1.code).isNotEqualTo(0);

        BaseResp bad2 = BaseResp.from(http.postJson(trialApply, sign.headers(data.teacherUserId, 1, "POST", trialApply), Map.of("reason", "E2E", "evidenceImageUrls", List.of())));
        assertThat(bad2.code).isNotEqualTo(0);

        BaseResp ok = BaseResp.from(http.postJson(trialApply, sign.headers(data.teacherUserId, 1, "POST", trialApply), Map.of("reason", "试课不通过", "evidenceImageUrls", List.of("https://example.com/1.jpg"))));
        assertThat(ok.code).isEqualTo(0);

        try (Db db = new Db(env)) {
            Long id = db.queryLong("SELECT id FROM refund_request WHERE course_id = ? ORDER BY id DESC LIMIT 1", courseId);
            assertThat(id).isNotNull();
            data.refundRequestId = id;
        }

        try (Db db = new Db(env)) {
            db.update("UPDATE course_enrollment SET trial_end_at = ? WHERE id = ?", LocalDateTime.now().minusDays(1), courseId);
        }
        BaseResp expired = BaseResp.from(http.postJson(trialApply, sign.headers(data.teacherUserId, 1, "POST", trialApply), Map.of("reason", "过期", "evidenceImageUrls", List.of("https://example.com/2.jpg"))));
        assertThat(expired.code).isNotEqualTo(0);
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

    private void prepay() throws Exception {
        URI prepay = URI.create(env.paymentBaseUrl + "/payment/prepay");
        BaseResp r = BaseResp.from(http.postJson(prepay, sign.headers(data.teacherUserId, 1, "POST", prepay), Map.of(
                "contextType", "BROKERAGE_ORDER",
                "contextId", data.brokerageOrderId,
                "channel", "WECHAT"
        )));
        assertThat(r.code).isEqualTo(0);
        data.paymentOrderNo = r.data.path("orderNo").asText();
    }

    private void markPaidUnlockChat() throws Exception {
        URI markPaid = URI.create(env.imBaseUrl + "/chat/brokerage/admin/order/" + data.brokerageOrderId + "/mark-paid");
        Map<String, String> headers = new HashMap<>(sign.headers(999999L, 0, "POST", markPaid));
        headers.put("X-Admin-Token", env.brokerageAdminToken);
        BaseResp r = BaseResp.from(http.postJson(markPaid, headers, Map.of()));
        assertThat(r.code).isEqualTo(0);
    }

    private Long createCollaborationProposal() throws Exception {
        URI create = URI.create(env.imBaseUrl + "/chat/collaboration/proposal");
        Map<String, Object> req = new HashMap<>();
        req.put("roomId", data.roomId);
        req.put("pricePerHour", "200");
        req.put("classTime", "周末");
        req.put("frequencyPerWeek", 2);
        BaseResp r = BaseResp.from(http.postJson(create, sign.headers(data.teacherUserId, 1, "POST", create), req));
        assertThat(r.code).isEqualTo(0);
        Long proposalId;
        try (Db db = new Db(env)) {
            proposalId = db.queryLong("SELECT id FROM collaboration_proposal WHERE room_id = ? ORDER BY id DESC LIMIT 1", data.roomId);
        }
        assertThat(proposalId).isNotNull();
        data.proposalIds.add(proposalId);
        return proposalId;
    }

    private void acceptCollaborationProposal(Long proposalId) throws Exception {
        URI accept = URI.create(env.imBaseUrl + "/chat/collaboration/proposal/" + proposalId + "/response");
        BaseResp r = BaseResp.from(http.postJson(accept, sign.headers(data.studentUserId, 2, "POST", accept), Map.of("action", "ACCEPT")));
        assertThat(r.code).isEqualTo(0);
    }
}
