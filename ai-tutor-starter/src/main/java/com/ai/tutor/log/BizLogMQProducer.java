package com.ai.tutor.log;

import com.ai.tutor.common.log.BizLogEvent;
import com.ai.tutor.common.log.BizLogMessage;
import com.ai.tutor.log.entity.BizLog;
import com.ai.tutor.log.mapper.BizLogMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 监听 BizLogEvent，入库 MySQL + 投递 RocketMQ。
 * MQ 不可用时降级到本地日志文件，但入库不受影响。
 */
@Component
public class BizLogMQProducer {

    private static final Logger log = LoggerFactory.getLogger(BizLogMQProducer.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final RocketMQTemplate rocketMQTemplate;
    private final BizLogMapper bizLogMapper;

    public BizLogMQProducer(RocketMQTemplate rocketMQTemplate, BizLogMapper bizLogMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.bizLogMapper = bizLogMapper;
    }

    @EventListener
    public void onBizLog(BizLogEvent event) {
        BizLogMessage msg = event.getLogMessage();
        String module = msg.getModule() == null ? "unknown" : msg.getModule();

        // 1. 入库 MySQL（不管 MQ 是否可用都执行）
        try {
            BizLog bizLog = BizLog.builder()
                    .module(module)
                    .traceId(msg.getTraceId())
                    .keyword(msg.getKeyword())
                    .level(msg.getLevel())
                    .content(msg.getContent())
                    .createdAt(parseTimestamp(msg.getTimestamp()))
                    .build();
            bizLogMapper.insert(bizLog);
        } catch (Exception e) {
            log.warn("业务日志入库失败: {}", e.getMessage());
        }

        // 2. 投递 MQ，失败则降级写本地文件
        try {
            rocketMQTemplate.send(BizLogConstants.TOPIC,
                    MessageBuilder.withPayload(msg)
                            .setHeader("KEYS", msg.getTraceId())
                            .build());
        } catch (Exception e) {
            MDC.put("bizModule", module);
            try {
                Logger fallbackLogger = LoggerFactory.getLogger("BIZ_LOG." + module);

                String formatted = String.format("[%s] [%s] [traceId=%s] [keyword=%s] %s",
                        module, msg.getTimestamp(), msg.getTraceId(), msg.getKeyword(), msg.getContent());

                String level = msg.getLevel() == null ? "INFO" : msg.getLevel().toUpperCase();
                switch (level) {
                    case "ERROR":
                        fallbackLogger.error(formatted);
                        break;
                    case "WARN":
                        fallbackLogger.warn(formatted);
                        break;
                    default:
                        fallbackLogger.info(formatted);
                        break;
                }
            } finally {
                MDC.remove("bizModule");
            }
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(timestamp, FMT);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
