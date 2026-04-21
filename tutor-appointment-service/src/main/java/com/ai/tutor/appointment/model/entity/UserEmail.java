package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmail {
    private Long id;
    private Long userId;
    private String emailType;
    private String email;
    private String emailMasked;
    private String verifyStatus;
    private LocalDateTime verifiedAt;
    private String bindSource;
    private String bounceStatus;
    private LocalDateTime lastNotifyAt;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
