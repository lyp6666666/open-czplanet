package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.InviteRewardRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InviteRewardRecordMapper {

    List<InviteRewardRecord> pageByInviterUid(@Param("inviterUid") Long inviterUid,
                                              @Param("cursor") Long cursor,
                                              @Param("pageSize") Integer pageSize,
                                              @Param("status") String status,
                                              @Param("scene") String scene);

    @Select("""
            SELECT COALESCE(SUM(reward_amount_fen), 0)
            FROM invite_reward_record
            WHERE inviter_uid = #{inviterUid}
            """)
    Long sumRewardAmountByInviterUid(@Param("inviterUid") Long inviterUid);

    @Select("""
            SELECT COALESCE(SUM(reward_amount_fen), 0)
            FROM invite_reward_record
            WHERE inviter_uid = #{inviterUid}
              AND status IN ('PENDING', 'SETTLEABLE', 'SETTLEMENT_PENDING')
            """)
    Long sumPendingAmountByInviterUid(@Param("inviterUid") Long inviterUid);

    @Select("""
            SELECT COUNT(*)
            FROM invite_reward_record
            WHERE inviter_uid = #{inviterUid}
              AND status IN ('PENDING', 'SETTLEABLE', 'SETTLEMENT_PENDING', 'PAID')
            """)
    Integer countEffectiveByInviterUid(@Param("inviterUid") Long inviterUid);

    @Select("""
            SELECT COUNT(*)
            FROM invite_reward_record
            WHERE inviter_uid = #{inviterUid}
              AND invitee_uid = #{inviteeUid}
              AND status IN ('PENDING', 'SETTLEABLE', 'SETTLEMENT_PENDING', 'PAID')
            """)
    Integer countEffectiveByInviterUidAndInviteeUid(@Param("inviterUid") Long inviterUid,
                                                    @Param("inviteeUid") Long inviteeUid);

    @Select("""
            SELECT DISTINCT inviter_uid
            FROM invite_reward_record
            WHERE status IN ('PENDING', 'SETTLEABLE')
              AND create_time < #{cutoff}
            ORDER BY inviter_uid ASC
            LIMIT #{limit}
            """)
    List<Long> listSettlementCandidateInviters(@Param("cutoff") java.time.LocalDateTime cutoff,
                                               @Param("limit") Integer limit);

    @Select("""
            SELECT COALESCE(SUM(reward_amount_fen), 0)
            FROM invite_reward_record
            WHERE inviter_uid = #{inviterUid}
              AND status IN ('PENDING', 'SETTLEABLE')
              AND create_time < #{cutoff}
            """)
    Long sumSettleableAmountByInviterUid(@Param("inviterUid") Long inviterUid,
                                         @Param("cutoff") java.time.LocalDateTime cutoff);

    int markSettlementPending(@Param("inviterUid") Long inviterUid,
                              @Param("settlementMonth") String settlementMonth,
                              @Param("cutoff") java.time.LocalDateTime cutoff);

    int insert(InviteRewardRecord record);
}
