package com.ai.tutor.liveclass.mapper;

import com.ai.tutor.liveclass.domain.entity.LiveClassSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface LiveClassSessionMapper {
    LiveClassSession selectByCourseId(@Param("courseId") Long courseId);

    LiveClassSession selectById(@Param("id") Long id);

    LiveClassSession selectByProviderRoomName(@Param("providerRoomName") String providerRoomName);

    java.util.List<LiveClassSession> listByParticipantUid(@Param("uid") Long uid);

    int insert(LiveClassSession session);

    int updateByCourseId(LiveClassSession session);

    int markParticipantJoined(@Param("id") Long id, @Param("uid") Long uid, @Param("joinedAt") LocalDateTime joinedAt);

    int markParticipantLeft(@Param("id") Long id, @Param("uid") Long uid, @Param("leftAt") LocalDateTime leftAt);

    int markEnded(@Param("id") Long id,
                  @Param("uid") Long uid,
                  @Param("endReason") String endReason,
                  @Param("endedAt") LocalDateTime endedAt);
}
