package com.ai.tutor.liveclass.mapper;

import com.ai.tutor.liveclass.domain.entity.LiveClassEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LiveClassEventMapper {
    int insert(LiveClassEvent event);

    List<LiveClassEvent> listBySessionId(@Param("sessionId") Long sessionId);
}
