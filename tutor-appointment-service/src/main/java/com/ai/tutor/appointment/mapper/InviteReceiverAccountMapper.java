package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.InviteReceiverAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InviteReceiverAccountMapper {

    @Select("SELECT * FROM invite_receiver_account WHERE user_id = #{userId} LIMIT 1")
    InviteReceiverAccount selectByUserId(@Param("userId") Long userId);

    int insert(InviteReceiverAccount account);

    @Update("""
            UPDATE invite_receiver_account
            SET receiver_name = #{receiverName},
                wechat_no = #{wechatNo},
                phone = #{phone},
                remark = #{remark},
                status = #{status},
                update_time = NOW(3)
            WHERE user_id = #{userId}
            """)
    int updateByUserId(InviteReceiverAccount account);
}
