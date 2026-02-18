package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentJobPostingMapper {

    int insert(StudentJobPosting posting);

    int updateById(StudentJobPosting posting);

    StudentJobPosting selectById(@Param("id") Long id);

    List<StudentJobPosting> listByParentId(@Param("parentId") Long parentId,
                                          @Param("cursor") Long cursor,
                                          @Param("pageSize") Integer pageSize);

    List<StudentJobPosting> listPublished(@Param("subjectId") Long subjectId,
                                          @Param("city") String city,
                                          @Param("classMode") String classMode,
                                          @Param("cursor") Long cursor,
                                          @Param("pageSize") Integer pageSize);

    List<StudentJobPosting> listPublishedSorted(@Param("subjectId") Long subjectId,
                                               @Param("city") String city,
                                               @Param("classMode") String classMode,
                                               @Param("keyword") String keyword,
                                               @Param("sort") String sort,
                                               @Param("cursor") Long cursor,
                                               @Param("pageSize") Integer pageSize);

    List<StudentJobPosting> listPublishedFiltered(@Param("subjectId") Long subjectId,
                                                 @Param("city") String city,
                                                 @Param("classMode") String classMode,
                                                 @Param("stageCode") String stageCode,
                                                 @Param("frequencyPerWeek") Integer frequencyPerWeek,
                                                 @Param("educationRequirement") String educationRequirement,
                                                 @Param("budgetMin") BigDecimal budgetMin,
                                                 @Param("budgetMax") BigDecimal budgetMax,
                                                 @Param("keyword") String keyword,
                                                 @Param("sort") String sort,
                                                 @Param("cursor") Long cursor,
                                                 @Param("pageSize") Integer pageSize);

    List<StudentJobPosting> searchPublishedByTitle(@Param("keyword") String keyword,
                                                  @Param("limit") Integer limit);
}
