package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请返利收款信息实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteReceiverAccount {

    private Long id;

    private Long userId;

    private String receiverName;

    private String wechatNo;

    private String phone;

    private String remark;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
