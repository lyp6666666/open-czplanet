package com.ai.tutor.videocallimservice.common.event.listener;

import com.ai.tutor.videocallimservice.chat.service.stream.ChatMessagePushService;
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

    @Resource
    private ChatMessagePushService chatMessagePushService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, classes = MessageSendEvent.class, fallbackExecution = true)
    public void messageRoute(MessageSendEvent event) {
        Long msgId = event.getMsgId();
        try {
            mqProducer.sendSecureMsg(MQConstant.SEND_MSG_TOPIC, new MsgSendMessageDTO(msgId), msgId);
        } catch (Exception e) {
            log.warn("send mq failed, msgId={}", msgId, e);
            try {
                // 远程开发环境里 MQ 偶发不可用时，这里直接降级走 SSE，避免前端必须刷新才能看到新消息。
                boolean pushed = chatMessagePushService.pushMessageById(msgId);
                if (!pushed) {
                    log.warn("fallback direct push skipped, msgId={}", msgId);
                }
            } catch (Exception fallbackEx) {
                log.error("fallback direct push failed, msgId={}", msgId, fallbackEx);
            }
        }
    }
}
