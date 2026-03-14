package com.ai.tutor.common.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务日志消息体
 * <p>
 * 格式: [module] [timestamp] [traceId] [keyword] content
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BizLogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模块名: appointment / im / admin / payment */
    private String module;

    /** 时间戳 yyyy-MM-dd HH:mm:ss.SSS */
    private String timestamp;

    /** 请求唯一ID，同一次请求的所有日志串联 */
    private String traceId;

    /** 业务关键词，调用方自己传，方便搜索：如 refund.approve / user.login / chat.send */
    private String keyword;

    /** 日志内容，自由格式 */
    private String content;

    /** 日志级别: INFO / WARN / ERROR */
    private String level;
}
