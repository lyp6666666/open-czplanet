package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.email.SendEmailCodeRequest;
import com.ai.tutor.appointment.model.dto.email.VerifyEmailRequest;
import com.ai.tutor.appointment.model.vo.email.EmailCodeVO;
import com.ai.tutor.appointment.model.vo.email.EmailReminderHintVO;
import com.ai.tutor.appointment.model.vo.email.InternalUserEmailsVO;
import com.ai.tutor.appointment.model.vo.email.UserEmailItemVO;
import com.ai.tutor.appointment.model.vo.email.UserEmailStatusVO;

public interface EmailAccountService {
    UserEmailStatusVO getStatus(Long userId);

    EmailCodeVO sendCode(Long userId, SendEmailCodeRequest request, String ip);

    UserEmailItemVO verify(Long userId, VerifyEmailRequest request);

    boolean deleteSummaryEmail(Long userId);

    EmailReminderHintVO getReminderHint(Long userId, String scene);

    InternalUserEmailsVO getInternalUserEmails(Long userId);
}
