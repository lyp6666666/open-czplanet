package com.ai.tutor.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RefundFlowE2eTest {

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
    void demandToPayToChatToRefundApply_shouldWorkAndCleanup() throws Exception {
        Assumptions.assumeTrue(env.gatewaySignSecret != null && env.gatewaySignSecret.length() >= 32, "E2E_GATEWAY_SIGN_SECRET too short");
        Assumptions.assumeTrue(env.brokerageAdminToken != null && !env.brokerageAdminToken.isEmpty(), "E2E_BROKERAGE_ADMIN_TOKEN missing");

        data = new E2eData();
        try (Db db = new Db(env)) {
            E2eDataHelper.prepareBaseData(db, data);
        }

        createApplicationAndAccept();
        prepay();
        markPaidUnlockChat();
        assertRefundEnabled();
        applyRefundAndRoomClosed();
    }

    private void createApplicationAndAccept() throws Exception {
        URI startChat = URI.create(env.imBaseUrl + "/chat/application/start-chat");
        Map<String, Object> req = new HashMap<>();
        req.put("receiverUid", data.studentUserId);
        req.put("contextType", "DEMAND");
        req.put("contextId", data.demandId);
        req.put("content", "E2E 申请：我可以辅导数学");
        BaseResp r1 = BaseResp.from(http.postJson(startChat, sign.headers(data.teacherUserId, 1, "POST", startChat), req));
        assertThat(r1.code).isEqualTo(0);
        JsonNode body = r1.data.path("message").path("body");
        Long applicationId = JsonExtract.extractApplicationId(body);
        if (applicationId == null) {
            throw new AssertionError("无法从 start-chat 响应解析 applicationId，body=" + body);
        }
        data.applicationId = applicationId;
        Long roomId = r1.data.path("message").path("roomId").asLong();
        assertThat(roomId).isNotNull();
        data.roomId = roomId;

        URI accept = URI.create(env.imBaseUrl + "/chat/application/" + data.applicationId + "/decision-message");
        Map<String, Object> acceptReq = new HashMap<>();
        acceptReq.put("action", "ACCEPT");
        BaseResp r2 = BaseResp.from(http.postJson(accept, sign.headers(data.studentUserId, 2, "POST", accept), acceptReq));
        assertThat(r2.code).isEqualTo(0);

        URI detail = URI.create(env.imBaseUrl + "/chat/application/" + data.applicationId);
        BaseResp r3 = BaseResp.from(http.get(detail, sign.headers(data.teacherUserId, 1, "GET", detail)));
        assertThat(r3.code).isEqualTo(0);
        data.brokerageOrderId = r3.data.path("orderId").asLong();
        assertThat(data.brokerageOrderId).isNotNull();
        assertThat(r3.data.path("chatAccessStatus").asText()).isIn("PAYMENT_REQUIRED", "CHAT_ENABLED");
    }

    private void prepay() throws Exception {
        URI prepay = URI.create(env.paymentBaseUrl + "/payment/prepay");
        Map<String, Object> req = new HashMap<>();
        req.put("contextType", "BROKERAGE_ORDER");
        req.put("contextId", data.brokerageOrderId);
        req.put("channel", "WECHAT");
        BaseResp r = BaseResp.from(http.postJson(prepay, sign.headers(data.teacherUserId, 1, "POST", prepay), req));
        assertThat(r.code).isEqualTo(0);
        String orderNo = r.data.path("orderNo").asText();
        assertThat(orderNo).isNotBlank();
        data.paymentOrderNo = orderNo;
    }

    private void markPaidUnlockChat() throws Exception {
        URI markPaid = URI.create(env.imBaseUrl + "/chat/brokerage/admin/order/" + data.brokerageOrderId + "/mark-paid");
        Map<String, String> headers = new HashMap<>(sign.headers(999999L, 0, "POST", markPaid));
        headers.put("X-Admin-Token", env.brokerageAdminToken);
        BaseResp r = BaseResp.from(http.postJson(markPaid, headers, Map.of()));
        assertThat(r.code).isEqualTo(0);

        URI detail = URI.create(env.imBaseUrl + "/chat/application/" + data.applicationId);
        BaseResp after = BaseResp.from(http.get(detail, sign.headers(data.teacherUserId, 1, "GET", detail)));
        assertThat(after.code).isEqualTo(0);
        assertThat(after.data.path("chatAccessStatus").asText()).isEqualTo("CHAT_ENABLED");
    }

    private void assertRefundEnabled() throws Exception {
        URI state = URI.create(env.imBaseUrl + "/chat/refund/state?roomId=" + data.roomId);
        BaseResp r = BaseResp.from(http.get(state, sign.headers(data.teacherUserId, 1, "GET", state)));
        assertThat(r.code).isEqualTo(0);
        assertThat(r.data.path("canApply").asBoolean()).isTrue();
        assertThat(r.data.path("hoverText").asText()).contains("申请退费");
    }

    private void applyRefundAndRoomClosed() throws Exception {
        URI apply = URI.create(env.imBaseUrl + "/chat/refund/apply");
        Map<String, Object> req = new HashMap<>();
        req.put("roomId", data.roomId);
        req.put("reason", "E2E 不合适");
        BaseResp r = BaseResp.from(http.postJson(apply, sign.headers(data.teacherUserId, 1, "POST", apply), req));
        assertThat(r.code).isEqualTo(0);

        try (Db db = new Db(env)) {
            Long roomStatus = db.queryLong("SELECT status FROM room WHERE id = ?", data.roomId);
            assertThat(roomStatus).isEqualTo(0L);
            Long refundId = db.queryLong("SELECT id FROM refund_request WHERE room_id = ? ORDER BY id DESC LIMIT 1", data.roomId);
            assertThat(refundId).isNotNull();
            data.refundRequestId = refundId;
        }

        URI send = URI.create(env.imBaseUrl + "/chat/msg");
        Map<String, Object> msg = new HashMap<>();
        msg.put("roomId", data.roomId);
        msg.put("msgType", 1);
        msg.put("body", "还能发消息吗");
        JsonNode raw = http.postJson(send, sign.headers(data.teacherUserId, 1, "POST", send), msg);
        BaseResp sendResp = BaseResp.from(raw);
        assertThat(sendResp.code).isNotEqualTo(0);
    }

}
