package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.HomeCarouselConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HomeCarouselConfigMapper {

    @Select("""
            SELECT id,
                   title,
                   subtitle,
                   image_object_key AS imageObjectKey,
                   link_type AS linkType,
                   link_url AS linkUrl,
                   sort_order AS sortOrder,
                   create_admin_id AS createAdminId,
                   update_admin_id AS updateAdminId,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM home_carousel_config
            ORDER BY sort_order ASC, id ASC
            """)
    List<HomeCarouselConfig> selectAll();
}
