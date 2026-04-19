package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.InviteCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface InviteCodeMapper {

    @Select("SELECT * FROM invite_code WHERE user_id = #{userId} LIMIT 1")
    InviteCode selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM invite_code WHERE invite_code = #{inviteCode} LIMIT 1")
    InviteCode selectByCode(@Param("inviteCode") String inviteCode);

    int insert(InviteCode inviteCode);

    int updateInviteCodeByUserId(@Param("userId") Long userId,
                                 @Param("inviteCode") String inviteCode,
                                 @Param("status") String status,
                                 @Param("updateTime") LocalDateTime updateTime);
}
