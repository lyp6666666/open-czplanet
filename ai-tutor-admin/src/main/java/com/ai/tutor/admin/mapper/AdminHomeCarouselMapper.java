package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.HomeCarouselConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminHomeCarouselMapper {

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

    @Select("SELECT COUNT(*) FROM home_carousel_config")
    long countAll();

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
            WHERE id = #{id}
            LIMIT 1
            """)
    HomeCarouselConfig selectById(@Param("id") Long id);

    @Insert("""
            INSERT INTO home_carousel_config (
              title, subtitle, image_object_key, link_type, link_url, sort_order, create_admin_id, update_admin_id
            ) VALUES (
              #{title}, #{subtitle}, #{imageObjectKey}, #{linkType}, #{linkUrl}, #{sortOrder}, #{createAdminId}, #{updateAdminId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HomeCarouselConfig config);

    @Delete("DELETE FROM home_carousel_config WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE home_carousel_config SET sort_order = #{sortOrder}, update_admin_id = #{updateAdminId} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder, @Param("updateAdminId") Long updateAdminId);
}
