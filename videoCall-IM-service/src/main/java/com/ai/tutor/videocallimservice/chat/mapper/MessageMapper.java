package com.ai.tutor.videocallimservice.chat.mapper;


import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessagePageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageSearchReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RoomUnreadCount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    CursorPageBaseResp<Message> getCursorPage(Long roomId, ChatMessagePageReq request);

    CursorPageBaseResp<Message> searchCursorPage(Long roomId, ChatMessageSearchReq request);

    @Select("SELECT * FROM message WHERE id = #{msgId}")
    Message getById(Long msgId);

    @Select("""
            SELECT COUNT(1)
            FROM message
            WHERE room_id = #{roomId}
              AND from_uid = #{fromUid}
              AND type = 2
              AND reply_msg_id = #{targetMsgId}
              AND status = 0
            """)
    Integer countRecallByTarget(@Param("roomId") Long roomId, @Param("fromUid") Long fromUid, @Param("targetMsgId") Long targetMsgId);

    List<RoomUnreadCount> listUnreadCounts(@Param("roomIds") List<Long> roomIds, @Param("uid") Long uid);


    void save(Message insert);
}
