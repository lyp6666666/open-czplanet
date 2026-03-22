package com.ai.tutor.videocallimservice.common.mapper;

import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImUserMapper {

    ImUser selectById(@Param("id") Long id);

    ImUser selectByUserTypeAndRefId(@Param("userType") Integer userType,
                                    @Param("refId") Long refId);
}
