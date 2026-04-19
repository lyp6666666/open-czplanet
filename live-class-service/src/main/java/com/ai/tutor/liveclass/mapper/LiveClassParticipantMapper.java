package com.ai.tutor.liveclass.mapper;

import com.ai.tutor.liveclass.domain.entity.LiveClassParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LiveClassParticipantMapper {
    LiveClassParticipant selectBySessionIdAndUid(@Param("sessionId") Long sessionId, @Param("uid") Long uid);

    List<LiveClassParticipant> listBySessionId(@Param("sessionId") Long sessionId);

    int insert(LiveClassParticipant participant);

    int updateJoinState(LiveClassParticipant participant);

    int updateLeaveState(LiveClassParticipant participant);
}
