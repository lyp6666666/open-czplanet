package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.RefundRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundRequestMapper {

    int insert(RefundRequest request);

    RefundRequest selectById(@Param("id") Long id);

    RefundRequest selectPendingByBrokerageOrderId(@Param("brokerageOrderId") Long brokerageOrderId);

    RefundRequest selectLatestByCourseIdOrRoomId(@Param("courseId") Long courseId, @Param("roomId") Long roomId);
}
