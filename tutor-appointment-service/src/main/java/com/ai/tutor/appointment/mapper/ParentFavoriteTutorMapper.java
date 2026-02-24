package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.ParentFavoriteTutor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ParentFavoriteTutorMapper {

    int insert(@Param("parentId") Long parentId, @Param("tutorId") Long tutorId);

    int delete(@Param("parentId") Long parentId, @Param("tutorId") Long tutorId);

    Integer exists(@Param("parentId") Long parentId, @Param("tutorId") Long tutorId);

    List<Long> listFavoritedTutorIds(@Param("parentId") Long parentId,
                                     @Param("tutorIds") List<Long> tutorIds);

    List<ParentFavoriteTutor> listByParent(@Param("parentId") Long parentId,
                                           @Param("cursor") Long cursor,
                                           @Param("pageSize") Integer pageSize);
}

