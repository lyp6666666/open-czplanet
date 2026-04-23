package com.ai.tutor.common.log;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求入口自动设置 traceId，请求结束自动清理。
 * 支持上游通过 X-Request-Id 传入，串联全链路。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            String traceId = request.getHeader("X-Request-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            BizLogger.setTraceId(traceId);
            MDC.put("requestId", traceId);
            MDC.put("traceId", traceId);

            response.setHeader("X-Request-Id", traceId);

            chain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
            BizLogger.removeTraceId();
        }
    }
}
