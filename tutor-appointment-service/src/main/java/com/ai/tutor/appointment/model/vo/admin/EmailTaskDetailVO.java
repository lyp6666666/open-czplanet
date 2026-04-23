package com.ai.tutor.appointment.model.vo.admin;

import com.ai.tutor.appointment.model.entity.EmailSendLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class EmailTaskDetailVO {
    private EmailTaskRowVO task;
    private Map<String, Object> payload;
    private List<EmailSendLog> logs;
}
