package com.ai.tutor.common.log;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 业务日志工具类 — 所有模块的 Service 层通过此类打日志
 */
@Component
public class BizLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * ThreadLocal 存放当前请求的 traceId，Filter 层设置，请求结束清理
     */
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private final ApplicationEventPublisher eventPublisher;

    public BizLogger(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static String getTraceId() {
        String id = TRACE_ID_HOLDER.get();
        if (id == null) {
            id = generateTraceId();
            TRACE_ID_HOLDER.set(id);
        }
        return id;
    }

    public static void removeTraceId() {
        TRACE_ID_HOLDER.remove();
    }


    public void info(String module, String keyword, String content) {
        publish(module, keyword, content, "INFO");
    }

    public void warn(String module, String keyword, String content) {
        publish(module, keyword, content, "WARN");
    }

    public void error(String module, String keyword, String content) {
        publish(module, keyword, content, "ERROR");
    }


    private void publish(String module, String keyword, String content, String level) {
        BizLogMessage msg = BizLogMessage.builder()
                .module(module)
                .timestamp(LocalDateTime.now().format(FMT))
                .traceId(getTraceId())
                .keyword(keyword)
                .content(content)
                .level(level)
                .build();
        eventPublisher.publishEvent(new BizLogEvent(this, msg));
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
