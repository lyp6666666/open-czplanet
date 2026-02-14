package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TutorAppointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TutorAppointmentMapper {

    int insert(TutorAppointment appointment);

    int updateById(TutorAppointment appointment);

    TutorAppointment selectById(@Param("id") Long id);

    /**
     * 幂等确认：仅当状态为“待确认”时将其更新为“已确认”。
     *
     * <p>返回更新行数：1 表示本次成功完成状态流转；0 表示已被并发请求处理（或状态不允许）。</p>
     */
    int acceptIfPending(@Param("id") Long id);

    int confirmReschedule(@Param("id") Long id);

    List<TutorAppointment> listByUser(@Param("uid") Long uid,
                                     @Param("status") Integer status,
                                     @Param("cursor") Long cursor,
                                     @Param("pageSize") Integer pageSize);
}
