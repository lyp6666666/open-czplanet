package com.ai.tutor.common.event;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邀请返利依赖的信息费支付成功事件。
 *
 * <p>该事件由 IM/支付链路在中介费订单支付成功后投递给 appointment 服务。
 * appointment 服务只依赖该 DTO 计算邀请返利，避免跨服务直接读取聊天域表结构。</p>
 */
@Data
public class InviteBrokeragePaidEvent implements Serializable {

    /**
     * 中介费订单 ID，作为返利幂等业务单号。
     */
    private Long brokerageOrderId;

    /**
     * 合作提案 ID，合作提案成单场景使用。
     */
    private Long proposalId;

    /**
     * 家教申请 ID，申请支付信息费场景使用。
     */
    private Long applicationId;

    /**
     * 聊天房间 ID，用于排查与运营追溯。
     */
    private Long roomId;

    /**
     * 教师用户 UID。
     */
    private Long teacherUid;

    /**
     * 学生用户 UID。
     */
    private Long studentUid;

    /**
     * 实际付款人 UID，当前信息费通常由教师支付。
     */
    private Long payerUid;

    /**
     * 信息费实付金额，单位分。
     */
    private Long amountFen;

    /**
     * 支付方式，如 WECHAT/ALIPAY。
     */
    private String payMethod;

    /**
     * 支付成功时间。
     */
    private LocalDateTime paidAt;

    /**
     * 事件来源，用于链路追踪。
     */
    private String source;
}
