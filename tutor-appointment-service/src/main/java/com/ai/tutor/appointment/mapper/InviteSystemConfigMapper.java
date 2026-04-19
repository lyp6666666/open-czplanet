package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.InviteSystemConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 系统邀请码配置 Mapper。
 */
@Mapper
public interface InviteSystemConfigMapper {

    @Select("SELECT id, enabled, system_invite_code AS systemInviteCode, system_invite_link AS systemInviteLink, tutor_info_fee_discount_rate AS tutorInfoFeeDiscountRate, student_reward_rate AS studentRewardRate, promo_title AS promoTitle, promo_desc AS promoDesc, create_time AS createTime, update_time AS updateTime FROM invite_system_config WHERE id = 1 LIMIT 1")
    InviteSystemConfig selectSingleton();

    @Insert("INSERT INTO invite_system_config (id, enabled, system_invite_code, system_invite_link, tutor_info_fee_discount_rate, student_reward_rate, promo_title, promo_desc, create_time, update_time) VALUES (1, #{enabled}, #{systemInviteCode}, #{systemInviteLink}, #{tutorInfoFeeDiscountRate}, #{studentRewardRate}, #{promoTitle}, #{promoDesc}, NOW(3), NOW(3))")
    int insertSingleton(InviteSystemConfig config);

    @Update("UPDATE invite_system_config SET enabled = #{enabled}, system_invite_code = #{systemInviteCode}, system_invite_link = #{systemInviteLink}, tutor_info_fee_discount_rate = #{tutorInfoFeeDiscountRate}, student_reward_rate = #{studentRewardRate}, promo_title = #{promoTitle}, promo_desc = #{promoDesc}, update_time = NOW(3) WHERE id = 1")
    int updateSingleton(InviteSystemConfig config);

    @Select("SELECT id, enabled, system_invite_code AS systemInviteCode, system_invite_link AS systemInviteLink, tutor_info_fee_discount_rate AS tutorInfoFeeDiscountRate, student_reward_rate AS studentRewardRate, promo_title AS promoTitle, promo_desc AS promoDesc, create_time AS createTime, update_time AS updateTime FROM invite_system_config WHERE UPPER(system_invite_code) = #{code} LIMIT 1")
    InviteSystemConfig selectByCode(@Param("code") String code);
}
