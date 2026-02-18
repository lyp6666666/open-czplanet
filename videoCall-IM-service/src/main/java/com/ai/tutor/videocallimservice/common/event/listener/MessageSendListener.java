package com.ai.tutor.videocallimservice.common.event.listener;

import com.ai.tutor.videocallimservice.chat.service.mq.MQProducer;
import com.ai.tutor.videocallimservice.common.constant.MQConstant;
import com.ai.tutor.videocallimservice.common.domain.dto.MsgSendMessageDTO;
import com.ai.tutor.videocallimservice.common.event.MessageSendEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
public class MessageSendListener {

    @Resource
    private MQProducer mqProducer;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, classes = MessageSendEvent.class, fallbackExecution = true)
    public void messageRoute(MessageSendEvent event) {
        Long msgId = event.getMsgId();
        try {
            mqProducer.sendSecureMsg(MQConstant.SEND_MSG_TOPIC, new MsgSendMessageDTO(msgId), msgId);
        } catch (Exception e) {
            log.warn("send mq failed, msgId={}", msgId, e);
        }
    }
}
