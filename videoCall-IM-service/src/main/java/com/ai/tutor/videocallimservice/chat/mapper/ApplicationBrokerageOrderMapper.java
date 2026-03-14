package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.ApplicationBrokerageOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApplicationBrokerageOrderMapper {
    void insert(ApplicationBrokerageOrder relation);

    ApplicationBrokerageOrder selectByApplicationId(@Param("applicationId") Long applicationId);

    ApplicationBrokerageOrder selectByOrderId(@Param("orderId") Long orderId);
}
