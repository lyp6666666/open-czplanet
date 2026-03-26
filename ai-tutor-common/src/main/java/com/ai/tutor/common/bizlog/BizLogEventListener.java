package com.ai.tutor.common.bizlog;

import com.ai.tutor.common.log.BizLogEvent;
import com.ai.tutor.common.log.BizLogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BizLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(BizLogEventListener.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final ApplicationContext applicationContext;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public BizLogEventListener(ApplicationContext applicationContext, ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.applicationContext = applicationContext;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    @EventListener
    public void onBizLog(BizLogEvent event) {
        BizLogMessage msg = event == null ? null : event.getLogMessage();
        if (msg == null) {
            return;
        }
        String module = msg.getModule() == null ? "unknown" : msg.getModule();

        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
            if (jdbcTemplate != null) {
                jdbcTemplate.update(
                        "INSERT INTO biz_log(module, trace_id, keyword, level, content, created_at) VALUES (?,?,?,?,?,?)",
                        module,
                        msg.getTraceId(),
                        msg.getKeyword(),
                        msg.getLevel(),
                        msg.getContent(),
                        Timestamp.valueOf(parseTimestamp(msg.getTimestamp()))
                );
            }
        } catch (Exception e) {
            log.warn("业务日志入库失败: {}", e.getMessage());
        }

        boolean sent = trySendRocketMq(msg);
        if (!sent) {
            writeFallbackFileLog(module, msg);
        }
    }

    private boolean trySendRocketMq(BizLogMessage msg) {
        Object rocketMQTemplate;
        try {
            rocketMQTemplate = applicationContext.getBean("rocketMQTemplate");
        } catch (Exception ignored) {
            return false;
        }
        try {
            rocketMQTemplate.getClass()
                    .getMethod("convertAndSend", String.class, Object.class)
                    .invoke(rocketMQTemplate, BizLogConstants.TOPIC, msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeFallbackFileLog(String module, BizLogMessage msg) {
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
