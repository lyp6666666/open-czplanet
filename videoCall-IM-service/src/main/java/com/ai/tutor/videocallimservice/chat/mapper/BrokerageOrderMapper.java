package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface BrokerageOrderMapper {
    void insert(BrokerageOrder order);

    BrokerageOrder selectById(@Param("id") Long id);

    BrokerageOrder selectByProposalId(@Param("proposalId") Long proposalId);

    BrokerageOrder selectByApplicationId(@Param("applicationId") Long applicationId);

    BrokerageOrder selectPaidByRoomId(@Param("roomId") Long roomId);

    int submitProof(@Param("id") Long id,
                    @Param("payMethod") String payMethod,
                    @Param("proofUrl") String proofUrl,
                    @Param("proofNote") String proofNote);

    int markPaid(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt);

    /**
     * 标记订单已支付（可同时写入支付方式）。
     *
     * @param id 订单ID
     * @param paidAt 支付时间
     * @param payMethod 支付方式（WECHAT/ALIPAY），可为空
     * @return 受影响行数
     */
    int markPaidWithMethod(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt, @Param("payMethod") String payMethod);

    /**
     * 付款人撤单：仅允许在待支付或已提交凭证状态撤销
     *
     * @param id 订单ID
     * @return 受影响行数
     */
    int cancel(@Param("id") Long id);

    /**
     * 进入退款审批流程（幂等/并发保护）。
     *
     * <p>仅允许从 PAID 状态进入，且 refund_locked=0 时才能更新成功。</p>
     *
     * @param id 订单ID
     * @param nextStatus 目标状态（REFUND_REVIEW/TRIAL_REFUND_REVIEW）
     * @return 受影响行数
     */
    int lockForRefund(@Param("id") Long id, @Param("nextStatus") String nextStatus);
}
