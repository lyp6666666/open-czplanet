package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.InviteRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InviteRelationMapper {

    @Select("SELECT * FROM invite_relation WHERE invitee_uid = #{inviteeUid} LIMIT 1")
    InviteRelation selectByInviteeUid(@Param("inviteeUid") Long inviteeUid);

    List<InviteRelation> pageByInviterUid(@Param("inviterUid") Long inviterUid,
                                          @Param("cursor") Long cursor,
                                          @Param("pageSize") Integer pageSize,
                                          @Param("status") String status);

    @Select("SELECT COUNT(*) FROM invite_relation WHERE inviter_uid = #{inviterUid} AND status = 'ACTIVE'")
    Integer countActiveByInviterUid(@Param("inviterUid") Long inviterUid);

    int insert(InviteRelation relation);
}
