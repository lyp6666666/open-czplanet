package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请返利收款信息视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteReceiverAccountVO {

    private String receiverName;

    private String wechatNo;

    private String phone;

    private String remark;

    private Boolean configured;
}
