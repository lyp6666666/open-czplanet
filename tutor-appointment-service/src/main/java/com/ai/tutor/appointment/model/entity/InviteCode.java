package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户邀请码实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCode {

    private Long id;

    private Long userId;

    private String inviteCode;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
