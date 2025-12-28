package com.ai.tutor.videocallimservice.chat.mapper;


import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessagePageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    CursorPageBaseResp<Message> getCursorPage(@Param("roomId") Long roomId,@Param("request") ChatMessagePageReq request);

    @Select("SELECT * FROM message WHERE id = #{msgId}")
    Message getById(Long msgId);


    void save(Message insert);
}
