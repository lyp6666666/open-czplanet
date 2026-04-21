package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CollaborationProposalMapper {
    void insert(CollaborationProposal proposal);
    CollaborationProposal selectById(@Param("id") Long id);
    CollaborationProposal selectLatestByRoomId(@Param("roomId") Long roomId);
    CollaborationProposal selectByFromUidAndClientRequestId(@Param("fromUid") Long fromUid, @Param("clientRequestId") String clientRequestId);
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("actorUid") Long actorUid,
                     @Param("actionTime") LocalDateTime actionTime);
    int updateContent(@Param("id") Long id,
                      @Param("fromUid") Long fromUid,
                      @Param("pricePerHour") String pricePerHour,
                      @Param("classTime") String classTime,
                      @Param("frequencyPerWeek") Integer frequencyPerWeek,
                      @Param("trialStartAt") LocalDateTime trialStartAt,
                      @Param("trialEndAt") LocalDateTime trialEndAt,
                      @Param("remark") String remark,
                      @Param("expireAt") LocalDateTime expireAt);
}
