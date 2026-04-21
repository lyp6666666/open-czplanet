package com.ai.tutor.appointment.model.vo.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailReminderHintVO {
    private Boolean show;
    private String level;
    private String title;
    private String description;
    private String actionText;
    private String actionTarget;
    private Boolean dismissible;
    private Integer cooldownDays;
}
