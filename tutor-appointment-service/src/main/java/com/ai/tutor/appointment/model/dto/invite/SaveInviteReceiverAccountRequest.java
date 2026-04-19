package com.ai.tutor.appointment.model.dto.invite;

import lombok.Data;

/**
 * 保存邀请返利收款信息请求。
 */
@Data
public class SaveInviteReceiverAccountRequest {

    private String receiverName;

    private String wechatNo;

    private String phone;

    private String remark;
}
