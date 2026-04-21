package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TutorAppointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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

    /**
     * 拒绝邀约：仅当状态为“待确认”时将其更新为“已拒绝”。
     *
     * <p>返回更新行数：1 表示成功；0 表示已被并发处理或状态不允许。</p>
     */
    int rejectIfPending(@Param("id") Long id);

    int completeIfAccepted(@Param("id") Long id);

    List<TutorAppointment> listByUser(@Param("uid") Long uid,
                                     @Param("status") Integer status,
                                     @Param("cursor") Long cursor,
                                     @Param("pageSize") Integer pageSize);

    /**
     * 查询当前用户在时间范围内的日程（用于日历展示）。
     *
     * <p>时间范围采用“区间相交”逻辑：eventStart &lt; end AND eventEnd &gt; start。</p>
     */
    List<TutorAppointment> listByUserAndTimeRange(@Param("uid") Long uid,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("includePending") Boolean includePending);

    List<TutorAppointment> listByCourseId(@Param("courseId") Long courseId);

    /**
     * 检查指定用户集合在给定时间段内是否存在已确认预约冲突。
     */
    int countAcceptedConflicts(@Param("uids") List<Long> uids,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    int countAcceptedConflictsExcept(@Param("uids") List<Long> uids,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("excludeId") Long excludeId);
}
