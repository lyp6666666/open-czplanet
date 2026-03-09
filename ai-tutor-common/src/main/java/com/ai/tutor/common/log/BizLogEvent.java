package com.ai.tutor.common.log;

import org.springframework.context.ApplicationEvent;

/**
 * 业务日志 Spring Event，用于解耦日志产生和 MQ 投递
 */
public class BizLogEvent extends ApplicationEvent {

    private final BizLogMessage logMessage;

    public BizLogEvent(Object source, BizLogMessage logMessage) {
        super(source);
        this.logMessage = logMessage;
    }

    public BizLogMessage getLogMessage() {
        return logMessage;
    }
}
