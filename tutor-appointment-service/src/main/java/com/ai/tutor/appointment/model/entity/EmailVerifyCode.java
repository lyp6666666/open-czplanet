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
public class EmailVerifyCode {
    private Long id;
    private Long userId;
    private String email;
    private String emailType;
    private String codeHash;
    private String scene;
    private LocalDateTime expireAt;
    private LocalDateTime verifiedAt;
    private Integer tryCount;
    private String sendIp;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
