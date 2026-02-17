package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TutorFavoriteDemand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TutorFavoriteDemandMapper {

    int insert(@Param("tutorId") Long tutorId, @Param("demandId") Long demandId);

    int delete(@Param("tutorId") Long tutorId, @Param("demandId") Long demandId);

    Integer exists(@Param("tutorId") Long tutorId, @Param("demandId") Long demandId);

    List<Long> listDemandIdsByTutor(@Param("tutorId") Long tutorId,
                                    @Param("cursor") Long cursor,
                                    @Param("pageSize") Integer pageSize);

    List<Long> listFavoritedDemandIds(@Param("tutorId") Long tutorId,
                                      @Param("demandIds") List<Long> demandIds);

    List<TutorFavoriteDemand> listByTutor(@Param("tutorId") Long tutorId,
                                          @Param("cursor") Long cursor,
                                          @Param("pageSize") Integer pageSize);
}

