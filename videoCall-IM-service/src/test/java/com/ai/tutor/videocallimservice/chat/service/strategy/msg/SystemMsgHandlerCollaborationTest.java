package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMsgHandlerCollaborationTest {

    @Test
    void shouldRenderCollaborationProposalBody() {
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL");
        body.setEventId(12L);
        body.setStatus("PENDING");
        body.setCreatorUserId(100L);
        body.setPricePerHour("200 元/小时");
        body.setClassTime("周二/四 19:00-21:00");
        body.setFrequencyPerWeek(2);

        Message msg = Message.builder().content("合作提案").extra(JSONUtil.toJsonStr(body)).build();
        Object out = new SystemMsgHandler().showMsg(msg);
        assertThat(out).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) out;
        assertThat(m.get("type")).isEqualTo("collaboration_proposal");
        assertThat(m.get("proposalId")).isEqualTo(12L);
        assertThat(m.get("status")).isEqualTo("PENDING");
        assertThat(m.get("creatorUserId")).isEqualTo(100L);
        assertThat(m.get("pricePerHour")).isEqualTo("200 元/小时");
        assertThat(m.get("classTime")).isEqualTo("周二/四 19:00-21:00");
        assertThat(m.get("frequencyPerWeek")).isEqualTo(2);
    }

    @Test
    void shouldRenderCollaborationStatusBody() {
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL_STATUS");
        body.setEventId(12L);
        body.setStatus("ACCEPTED");
        body.setActorUserId(200L);

        Message msg = Message.builder().content("合作提案：ACCEPTED").extra(JSONUtil.toJsonStr(body)).build();
        Object out = new SystemMsgHandler().showMsg(msg);
        assertThat(out).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) out;
        assertThat(m.get("type")).isEqualTo("collaboration_status");
        assertThat(m.get("proposalId")).isEqualTo(12L);
        assertThat(m.get("status")).isEqualTo("ACCEPTED");
        assertThat(m.get("actorUserId")).isEqualTo(200L);
    }
}
