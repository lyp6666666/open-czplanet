package com.ai.tutor.videocallimservice.common.mapper;

import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImUserMapper {

    @Select("SELECT id, user_type AS userType, ref_id AS refId, status FROM user WHERE id = #{id} LIMIT 1")
    ImUser selectById(@Param("id") Long id);

    @Select("SELECT id, user_type AS userType, ref_id AS refId, status FROM user WHERE user_type = #{userType} AND ref_id = #{refId} LIMIT 1")
    ImUser selectByUserTypeAndRefId(@Param("userType") Integer userType, @Param("refId") Long refId);
}

