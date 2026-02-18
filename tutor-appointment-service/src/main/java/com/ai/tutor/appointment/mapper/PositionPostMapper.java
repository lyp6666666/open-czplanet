package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.PositionPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PositionPostMapper {

    List<PositionPost> selectEnabledAll();

    List<PositionPost> searchEnabledByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);

    List<PositionPost> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 选择一个“可用叶子科目”（没有启用子节点的科目），用于兜底场景。
     */
    Long selectFirstEnabledLeafId();
}
