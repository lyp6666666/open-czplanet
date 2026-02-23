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
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("actorUid") Long actorUid,
                     @Param("actionTime") LocalDateTime actionTime);
}
