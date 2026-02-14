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
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}

