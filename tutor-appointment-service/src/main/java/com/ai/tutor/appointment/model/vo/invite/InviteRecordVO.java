package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请记录视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteRecordVO {

    private Long inviteeUid;

    private String inviteeDisplayName;

    private String inviteePhoneMasked;

    private Integer inviteeUserType;

    private String registeredAt;

    private String status;

    private Boolean hasReward;
}
