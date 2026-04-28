package com.ai.tutor.liveclass.mapper;

import com.ai.tutor.liveclass.domain.entity.LiveWhiteboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveWhiteboardMapper {
    LiveWhiteboard selectBySessionId(@Param("sessionId") Long sessionId);

    int insert(LiveWhiteboard whiteboard);

    int updateSnapshot(@Param("id") Long id,
                       @Param("sceneJson") String sceneJson,
                       @Param("sceneVersion") Long sceneVersion,
                       @Param("updatedByUid") Long updatedByUid,
                       @Param("finalized") Boolean finalized);
}
