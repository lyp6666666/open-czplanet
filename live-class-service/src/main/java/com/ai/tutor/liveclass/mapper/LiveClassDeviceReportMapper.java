package com.ai.tutor.liveclass.mapper;

import com.ai.tutor.liveclass.domain.entity.LiveClassDeviceReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LiveClassDeviceReportMapper {
    int insert(LiveClassDeviceReport report);
}
