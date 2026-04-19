package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 邀请规则视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteRulesVO {

    private Double teacherRewardRate;

    private Double studentRewardRate;

    private Integer settlementDay;

    private Long minSettlementAmountFen;

    private Boolean enabled;

    private String receiverHint;

    private InviteSystemConfigVO systemInviteConfig;

    private List<String> ruleTextList;
}
