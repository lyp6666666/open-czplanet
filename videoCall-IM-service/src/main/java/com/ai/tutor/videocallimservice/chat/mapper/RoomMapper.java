package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMapper {

    Room selectById(@Param("id") Long id);

    Room selectByIdForUpdate(@Param("id") Long id);

    Room selectByTeacherAndStudent(@Param("teacherProfileId") Long teacherProfileId,
                                   @Param("studentProfileId") Long studentProfileId);

    int insert(Room room);

    int updateAfterSend(@Param("roomId") Long roomId, @Param("lastMsgId") Long lastMsgId);

    int closeRoom(@Param("roomId") Long roomId);

    int reopenRoom(@Param("roomId") Long roomId);

    List<Room> listByTeacherProfileId(@Param("teacherProfileId") Long teacherProfileId,
                                      @Param("cursor") Long cursor,
                                      @Param("pageSize") Integer pageSize);

    List<Room> listByStudentProfileId(@Param("studentProfileId") Long studentProfileId,
                                      @Param("cursor") Long cursor,
                                      @Param("pageSize") Integer pageSize);

    List<Long> listPeerUserIdsByUid(@Param("uid") Long uid);
}
