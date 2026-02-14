package com.ai.tutor.videocallimservice.chat.service.strategy;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;

import java.util.HashMap;
import java.util.Map;

public class MsgHandlerFactory {
    private static final Map<Integer, AbstractMsgHandler> STRATEGY_MAP = new HashMap<>();

    public static void register(Integer code, AbstractMsgHandler strategy) {
        STRATEGY_MAP.put(code, strategy);
    }

    public static AbstractMsgHandler getStrategyNoNull(Integer code) {
        AbstractMsgHandler strategy = STRATEGY_MAP.get(code);
        ThrowUtils.throwIf(strategy == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        return strategy;
    }
}
