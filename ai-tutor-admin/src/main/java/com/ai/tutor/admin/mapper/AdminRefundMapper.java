package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminRefundMapper extends BaseMapper<BrokerageOrder> {

    @Select("SELECT * FROM brokerage_order WHERE status = 'DISPUTE' ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<BrokerageOrder> listRefundDisputes(@Param("offset") long offset, @Param("limit") long limit);

    @Select("SELECT COUNT(*) FROM brokerage_order WHERE status = 'DISPUTE'")
    long countRefundDisputes();

    @Select("SELECT * FROM brokerage_order WHERE id = #{id}")
    BrokerageOrder selectById(Long id);

    @Update("UPDATE brokerage_order SET status = 'REFUNDED', update_time = NOW() WHERE id = #{id} AND status = 'DISPUTE'")
    int approveRefund(Long id);

    @Update("UPDATE brokerage_order SET status = 'PAID', update_time = NOW() WHERE id = #{id} AND status = 'DISPUTE'")
    int rejectRefund(Long id);
}
