package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.EmailSendLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailSendLogMapper {
    int insert(EmailSendLog log);
}
