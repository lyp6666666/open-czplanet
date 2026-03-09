package com.ai.tutor.log;

import com.ai.tutor.common.log.BizLogMessage;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 消费 MQ 业务日志，按模块写入不同的日志文件。
 * <p>
 * 入库逻辑已移至 BizLogMQProducer（业务机器端直接入库），
 * 此处只负责写日志文件。
 */
@Component
@RocketMQMessageListener(
        topic = BizLogConstants.TOPIC,
        consumerGroup = BizLogConstants.CONSUMER_GROUP
)
public class BizLogConsumer implements RocketMQListener<BizLogMessage> {

    @Override
    public void onMessage(BizLogMessage msg) {
        if (msg == null) return;

        String module = msg.getModule() == null ? "unknown" : msg.getModule();

        MDC.put("bizModule", module);
        try {
            Logger moduleLogger = LoggerFactory.getLogger("BIZ_LOG." + module);

            String formatted = String.format("[%s] [%s] [traceId=%s] [keyword=%s] %s",
                    module,
                    msg.getTimestamp(),
                    msg.getTraceId(),
                    msg.getKeyword(),
                    msg.getContent());

            String level = msg.getLevel() == null ? "INFO" : msg.getLevel().toUpperCase();
            switch (level) {
                case "ERROR":
                    moduleLogger.error(formatted);
                    break;
                case "WARN":
                    moduleLogger.warn(formatted);
                    break;
                default:
                    moduleLogger.info(formatted);
                    break;
            }
        } finally {
            MDC.remove("bizModule");
        }
    }
}
