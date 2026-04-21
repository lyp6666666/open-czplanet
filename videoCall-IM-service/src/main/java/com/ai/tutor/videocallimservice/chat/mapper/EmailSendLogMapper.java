package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.EmailSendLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailSendLogMapper {
    int insert(EmailSendLog log);
}
