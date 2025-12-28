package com.ai.tutor.videocallimservice.chat.service.strategy.msg;

import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.enums.MessageTypeEnum;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.TextMsgReq;
import com.ai.tutor.videocallimservice.chat.service.strategy.AbstractMsgHandler;

public class TextMsgHandler extends AbstractMsgHandler<TextMsgReq> {
    @Override
    protected MessageTypeEnum getMsgTypeEnum() {
        return null;
    }

    @Override
    protected void saveMsg(Message message, TextMsgReq body) {

    }

    @Override
    public Object showMsg(Message msg) {
        return null;
    }

    @Override
    public Object showReplyMsg(Message msg) {
        return null;
    }

    @Override
    public String showContactMsg(Message msg) {
        return null;
    }
}
