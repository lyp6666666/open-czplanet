package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TeacherJobPosting;
import com.ai.tutor.appointment.model.dto.home.HomeHotTutorAggRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherJobPostingMapper {

    int insert(TeacherJobPosting posting);

    int updateById(TeacherJobPosting posting);

    TeacherJobPosting selectById(@Param("id") Long id);

    List<TeacherJobPosting> listByTutorId(@Param("tutorId") Long tutorId,
                                         @Param("cursor") Long cursor,
                                         @Param("pageSize") Integer pageSize);

    List<TeacherJobPosting> listPublished(@Param("subjectId") Long subjectId,
                                         @Param("city") String city,
                                         @Param("mode") String mode,
                                         @Param("cursor") Long cursor,
                                         @Param("pageSize") Integer pageSize);

    List<TeacherJobPosting> listPublishedSorted(@Param("subjectId") Long subjectId,
                                               @Param("city") String city,
                                               @Param("mode") String mode,
                                               @Param("sort") String sort,
                                               @Param("cursor") Long cursor,
                                               @Param("pageSize") Integer pageSize);

    List<TeacherJobPosting> searchPublishedByTitle(@Param("keyword") String keyword,
                                                  @Param("limit") Integer limit);

    List<HomeHotTutorAggRow> listHotTutors(@Param("subjectId") Long subjectId,
                                          @Param("city") String city,
                                          @Param("mode") String mode,
                                          @Param("cursor") Long cursor,
                                          @Param("pageSize") Integer pageSize);

    List<HomeHotTutorAggRow> listHotTutorsCityHybrid(@Param("subjectId") Long subjectId,
                                                     @Param("city") String city,
                                                     @Param("cursor") Long cursor,
                                                     @Param("pageSize") Integer pageSize);

    List<TeacherJobPosting> listTopNByTutorIds(@Param("tutorIds") List<Long> tutorIds,
                                              @Param("topN") Integer topN);
}
