package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.InviteSettlementOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InviteSettlementOrderMapper {

    List<InviteSettlementOrder> pageByUserId(@Param("userId") Long userId,
                                             @Param("cursor") Long cursor,
                                             @Param("pageSize") Integer pageSize);

    @Select("SELECT * FROM invite_settlement_order WHERE user_id = #{userId} AND settlement_month = #{settlementMonth} LIMIT 1")
    InviteSettlementOrder selectByUserIdAndMonth(@Param("userId") Long userId,
                                                 @Param("settlementMonth") String settlementMonth);

    int insert(InviteSettlementOrder order);
}
