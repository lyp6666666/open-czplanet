package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.vo.AdminInviteRelationVO;
import com.ai.tutor.admin.model.vo.AdminInviteRewardVO;
import com.ai.tutor.admin.model.vo.AdminInviteSettlementVO;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminInviteMapper {

    @Select({
            "<script>",
            "SELECT ir.id, ir.inviter_uid AS inviterUid, inviter.name AS inviterName, inviter.phone AS inviterPhone,",
            "ir.invitee_uid AS inviteeUid, invitee.name AS inviteeName, invitee.phone AS inviteePhone, invitee.user_type AS inviteeUserType,",
            "ir.invite_code AS inviteCode, ir.bind_source AS bindSource, ir.status, ir.risk_flag AS riskFlag, ir.bind_time AS bindTime, ir.create_time AS createTime",
            "FROM invite_relation ir",
            "LEFT JOIN user inviter ON inviter.id = ir.inviter_uid",
            "LEFT JOIN user invitee ON invitee.id = ir.invitee_uid",
            "WHERE 1=1",
            "<if test='inviterUid != null'> AND ir.inviter_uid = #{inviterUid} </if>",
            "<if test='inviteeUid != null'> AND ir.invitee_uid = #{inviteeUid} </if>",
            "<if test='status != null and status != \"\"'> AND ir.status = #{status} </if>",
            "ORDER BY ir.id DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<AdminInviteRelationVO> listRelations(@Param("offset") long offset,
                                              @Param("limit") int limit,
                                              @Param("inviterUid") Long inviterUid,
                                              @Param("inviteeUid") Long inviteeUid,
                                              @Param("status") String status);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM invite_relation ir",
            "WHERE 1=1",
            "<if test='inviterUid != null'> AND ir.inviter_uid = #{inviterUid} </if>",
            "<if test='inviteeUid != null'> AND ir.invitee_uid = #{inviteeUid} </if>",
            "<if test='status != null and status != \"\"'> AND ir.status = #{status} </if>",
            "</script>"
    })
    long countRelations(@Param("inviterUid") Long inviterUid,
                        @Param("inviteeUid") Long inviteeUid,
                        @Param("status") String status);

    @Select({
            "<script>",
            "SELECT rr.id, rr.inviter_uid AS inviterUid, inviter.name AS inviterName, inviter.phone AS inviterPhone,",
            "rr.invitee_uid AS inviteeUid, invitee.name AS inviteeName, invitee.phone AS inviteePhone,",
            "rr.reward_scene AS rewardScene, rr.biz_order_type AS bizOrderType, rr.biz_order_id AS bizOrderId,",
            "rr.base_amount_fen AS baseAmountFen, rr.reward_rate AS rewardRate, rr.reward_amount_fen AS rewardAmountFen,",
            "rr.status, rr.settlement_month AS settlementMonth, rr.create_time AS createTime",
            "FROM invite_reward_record rr",
            "LEFT JOIN user inviter ON inviter.id = rr.inviter_uid",
            "LEFT JOIN user invitee ON invitee.id = rr.invitee_uid",
            "WHERE 1=1",
            "<if test='inviterUid != null'> AND rr.inviter_uid = #{inviterUid} </if>",
            "<if test='inviteeUid != null'> AND rr.invitee_uid = #{inviteeUid} </if>",
            "<if test='status != null and status != \"\"'> AND rr.status = #{status} </if>",
            "<if test='scene != null and scene != \"\"'> AND rr.reward_scene = #{scene} </if>",
            "<if test='settlementMonth != null and settlementMonth != \"\"'> AND rr.settlement_month = #{settlementMonth} </if>",
            "ORDER BY rr.id DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<AdminInviteRewardVO> listRewards(@Param("offset") long offset,
                                          @Param("limit") int limit,
                                          @Param("inviterUid") Long inviterUid,
                                          @Param("inviteeUid") Long inviteeUid,
                                          @Param("status") String status,
                                          @Param("scene") String scene,
                                          @Param("settlementMonth") String settlementMonth);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM invite_reward_record rr",
            "WHERE 1=1",
            "<if test='inviterUid != null'> AND rr.inviter_uid = #{inviterUid} </if>",
            "<if test='inviteeUid != null'> AND rr.invitee_uid = #{inviteeUid} </if>",
            "<if test='status != null and status != \"\"'> AND rr.status = #{status} </if>",
            "<if test='scene != null and scene != \"\"'> AND rr.reward_scene = #{scene} </if>",
            "<if test='settlementMonth != null and settlementMonth != \"\"'> AND rr.settlement_month = #{settlementMonth} </if>",
            "</script>"
    })
    long countRewards(@Param("inviterUid") Long inviterUid,
                      @Param("inviteeUid") Long inviteeUid,
                      @Param("status") String status,
                      @Param("scene") String scene,
                      @Param("settlementMonth") String settlementMonth);

    @Select({
            "<script>",
            "SELECT so.id, so.user_id AS userId, u.name AS userName, u.phone AS userPhone,",
            "so.settlement_month AS settlementMonth, so.total_amount_fen AS totalAmountFen, so.paid_amount_fen AS paidAmountFen,",
            "so.status, so.receiver_snapshot_json AS receiverSnapshotJson, so.fail_reason AS failReason,",
            "so.pay_time AS payTime, so.create_time AS createTime, so.update_time AS updateTime",
            "FROM invite_settlement_order so",
            "LEFT JOIN user u ON u.id = so.user_id",
            "WHERE 1=1",
            "<if test='userId != null'> AND so.user_id = #{userId} </if>",
            "<if test='status != null and status != \"\"'> AND so.status = #{status} </if>",
            "<if test='settlementMonth != null and settlementMonth != \"\"'> AND so.settlement_month = #{settlementMonth} </if>",
            "ORDER BY so.id DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<AdminInviteSettlementVO> listSettlements(@Param("offset") long offset,
                                                 @Param("limit") int limit,
                                                 @Param("userId") Long userId,
                                                 @Param("status") String status,
                                                 @Param("settlementMonth") String settlementMonth);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM invite_settlement_order so",
            "WHERE 1=1",
            "<if test='userId != null'> AND so.user_id = #{userId} </if>",
            "<if test='status != null and status != \"\"'> AND so.status = #{status} </if>",
            "<if test='settlementMonth != null and settlementMonth != \"\"'> AND so.settlement_month = #{settlementMonth} </if>",
            "</script>"
    })
    long countSettlements(@Param("userId") Long userId,
                          @Param("status") String status,
                          @Param("settlementMonth") String settlementMonth);

    @Select("SELECT id, user_id AS userId, settlement_month AS settlementMonth, total_amount_fen AS totalAmountFen, paid_amount_fen AS paidAmountFen, status, receiver_snapshot_json AS receiverSnapshotJson, fail_reason AS failReason, pay_time AS payTime, create_time AS createTime, update_time AS updateTime FROM invite_settlement_order WHERE id = #{id} LIMIT 1")
    AdminInviteSettlementVO selectSettlementById(@Param("id") Long id);

    @Update("UPDATE invite_settlement_order SET status = 'PAID', paid_amount_fen = total_amount_fen, pay_time = #{payTime}, fail_reason = NULL, update_time = NOW(3) WHERE id = #{id} AND status IN ('CREATED', 'PAYING', 'FAILED')")
    int markSettlementPaid(@Param("id") Long id, @Param("payTime") LocalDateTime payTime);

    @Update("UPDATE invite_settlement_order SET status = 'FAILED', fail_reason = #{reason}, update_time = NOW(3) WHERE id = #{id} AND status IN ('CREATED', 'PAYING')")
    int markSettlementFailed(@Param("id") Long id, @Param("reason") String reason);

    @Update("UPDATE invite_reward_record SET status = 'PAID', update_time = NOW(3) WHERE inviter_uid = #{userId} AND settlement_month = #{settlementMonth} AND status = 'SETTLEMENT_PENDING'")
    int markRewardsPaid(@Param("userId") Long userId, @Param("settlementMonth") String settlementMonth);

    @Select("SELECT enabled, system_invite_code AS systemInviteCode, system_invite_link AS systemInviteLink, tutor_info_fee_discount_rate AS tutorInfoFeeDiscountRate, student_reward_rate AS studentRewardRate, promo_title AS promoTitle, promo_desc AS promoDesc FROM invite_system_config WHERE id = 1 LIMIT 1")
    AdminInviteSystemConfigVO selectSystemConfig();

    @Update("UPDATE invite_system_config SET enabled = #{enabled}, system_invite_code = #{systemInviteCode}, system_invite_link = #{systemInviteLink}, tutor_info_fee_discount_rate = #{tutorInfoFeeDiscountRate}, student_reward_rate = #{studentRewardRate}, promo_title = #{promoTitle}, promo_desc = #{promoDesc}, update_time = NOW(3) WHERE id = 1")
    int updateSystemConfig(AdminInviteSystemConfigVO config);

    @Insert("INSERT INTO invite_system_config (id, enabled, system_invite_code, system_invite_link, tutor_info_fee_discount_rate, student_reward_rate, promo_title, promo_desc, create_time, update_time) VALUES (1, #{enabled}, #{systemInviteCode}, #{systemInviteLink}, #{tutorInfoFeeDiscountRate}, #{studentRewardRate}, #{promoTitle}, #{promoDesc}, NOW(3), NOW(3))")
    int insertSystemConfig(AdminInviteSystemConfigVO config);
}
