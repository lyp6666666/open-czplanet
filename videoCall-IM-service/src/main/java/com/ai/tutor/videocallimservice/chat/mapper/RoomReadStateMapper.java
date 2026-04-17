package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.RoomReadState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomReadStateMapper extends BaseMapper<RoomReadState> {
    RoomReadState getByRoomAndUid(@Param("roomId") Long roomId, @Param("uid") Long uid);

    List<RoomReadState> listByRoomIdsAndUid(@Param("roomIds") List<Long> roomIds, @Param("uid") Long uid);

    List<RoomReadState> listByRoomIdsAndUids(@Param("roomIds") List<Long> roomIds, @Param("uids") List<Long> uids);

    int upsertReadState(@Param("roomId") Long roomId, @Param("uid") Long uid, @Param("lastReadMsgId") Long lastReadMsgId);
}
