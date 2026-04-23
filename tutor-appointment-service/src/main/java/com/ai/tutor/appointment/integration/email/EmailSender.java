package com.ai.tutor.appointment.integration.email;

import com.ai.tutor.appointment.integration.email.dto.EmailSendRequest;
import com.ai.tutor.appointment.integration.email.dto.EmailSendResponse;

public interface EmailSender {

    /**
     * 发送 HTML 邮件，返回第三方请求结果。
     */
    EmailSendResponse send(EmailSendRequest request);
}
