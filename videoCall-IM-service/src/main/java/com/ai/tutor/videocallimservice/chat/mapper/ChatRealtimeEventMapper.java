package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.ChatRealtimeEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRealtimeEventMapper extends BaseMapper<ChatRealtimeEvent> {
    int insertEvent(ChatRealtimeEvent event);

    List<ChatRealtimeEvent> listAfter(@Param("targetUid") Long targetUid,
                                      @Param("lastEventId") Long lastEventId,
                                      @Param("limit") Integer limit);

    Long selectLatestEventId(@Param("targetUid") Long targetUid);
}
