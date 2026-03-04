package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminMessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM message WHERE room_id = #{roomId} ORDER BY create_time ASC")
    List<Message> listByRoomId(@Param("roomId") Long roomId);
}
