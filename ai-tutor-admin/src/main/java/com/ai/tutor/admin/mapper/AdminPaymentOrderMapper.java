package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.PaymentOrderRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminPaymentOrderMapper extends BaseMapper<PaymentOrderRecord> {

    @Select({
            "<script>",
            "SELECT * FROM payment_order",
            "WHERE 1=1",
            "<if test='orderNo != null and orderNo != \"\"'> AND order_no = #{orderNo} </if>",
            "<if test='userId != null'> AND user_id = #{userId} </if>",
            "<if test='contextType != null and contextType != \"\"'> AND context_type = #{contextType} </if>",
            "<if test='contextId != null'> AND context_id = #{contextId} </if>",
            "<if test='channel != null and channel != \"\"'> AND channel = #{channel} </if>",
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>",
            "<if test='startTime != null'> AND create_time &gt;= #{startTime} </if>",
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>",
            "ORDER BY create_time DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<PaymentOrderRecord> list(@Param("offset") long offset,
                                 @Param("limit") long limit,
                                 @Param("orderNo") String orderNo,
                                 @Param("userId") Long userId,
                                 @Param("contextType") String contextType,
                                 @Param("contextId") Long contextId,
                                 @Param("channel") String channel,
                                 @Param("status") String status,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM payment_order",
            "WHERE 1=1",
            "<if test='orderNo != null and orderNo != \"\"'> AND order_no = #{orderNo} </if>",
            "<if test='userId != null'> AND user_id = #{userId} </if>",
            "<if test='contextType != null and contextType != \"\"'> AND context_type = #{contextType} </if>",
            "<if test='contextId != null'> AND context_id = #{contextId} </if>",
            "<if test='channel != null and channel != \"\"'> AND channel = #{channel} </if>",
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>",
            "<if test='startTime != null'> AND create_time &gt;= #{startTime} </if>",
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>",
            "</script>"
    })
    long count(@Param("orderNo") String orderNo,
               @Param("userId") Long userId,
               @Param("contextType") String contextType,
               @Param("contextId") Long contextId,
               @Param("channel") String channel,
               @Param("status") String status,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM payment_order WHERE order_no = #{orderNo} LIMIT 1")
    PaymentOrderRecord getByOrderNo(@Param("orderNo") String orderNo);
}

