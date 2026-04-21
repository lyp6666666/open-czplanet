package com.ai.tutor.appointment.model.vo.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailCodeVO {
    private Integer cooldownSeconds;
    private Integer expireSeconds;
}
