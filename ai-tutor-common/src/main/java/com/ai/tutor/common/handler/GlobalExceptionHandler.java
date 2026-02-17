package com.ai.tutor.common.handler;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.exception.FrequencyControlException;
import com.ai.tutor.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 全局异常处理器：
 * 1. 保证接口在发生异常时，依然返回统一的 BaseResponse 结构，便于前端处理。
 * 2. 对业务异常（BusinessException）按业务错误码返回。
 * 3. 对参数校验异常给出清晰可读的错误信息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：直接返回业务错误码与消息。
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, msg={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 限流异常：返回限流提示。
     */
    @ExceptionHandler(FrequencyControlException.class)
    public BaseResponse<?> handleFrequencyControlException(FrequencyControlException e) {
        log.warn("FrequencyControlException: code={}, msg={}", e.getErrorCode(), e.getErrorMsg());
        if (e.getErrorCode() != null) {
            return ResultUtils.error(e.getErrorCode(), e.getErrorMsg());
        }
        return ResultUtils.error(ErrorCode.FORBIDDEN_ERROR, e.getErrorMsg());
    }

    /**
     * 参数校验异常（@Valid）：拼接出最关键的报错信息，方便快速定位问题。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        log.warn("MethodArgumentNotValidException: {}", message);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 兜底异常：避免把堆栈信息直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        // #region debug-point
        String dbgUrl = System.getProperty("TRAE_DEBUG_URL");
        if (dbgUrl != null && !dbgUrl.isBlank()) {
            try {
                String exName = e.getClass().getName();
                String exMsg = e.getMessage() == null ? "" : e.getMessage();
                String top = "";
                StackTraceElement[] st = e.getStackTrace();
                if (st != null && st.length > 0 && st[0] != null) {
                    top = st[0].toString();
                }
                String body = "{\"ts\":\"" + Instant.now() + "\",\"event\":\"unhandled_exception\""
                        + ",\"name\":\"" + escapeJson(exName) + "\""
                        + ",\"msg\":\"" + escapeJson(exMsg) + "\""
                        + ",\"top\":\"" + escapeJson(top) + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(dbgUrl.trim()))
                        .timeout(java.time.Duration.ofSeconds(2))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                HttpClient.newHttpClient().sendAsync(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        }
        // #endregion debug-point
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') out.append("\\\\");
            else if (c == '"') out.append("\\\"");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else out.append(c);
        }
        return out.toString();
    }
}
