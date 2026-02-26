package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TutorApplicationMapper {
    void insert(TutorApplication application);

    TutorApplication selectById(@Param("id") Long id);

    TutorApplication selectBySenderAndClientRequestId(@Param("senderUid") Long senderUid, @Param("clientRequestId") String clientRequestId);

    List<TutorApplication> listBySender(@Param("senderUid") Long senderUid, @Param("cursor") Long cursor, @Param("pageSize") Integer pageSize);

    List<TutorApplication> listByReceiver(@Param("receiverUid") Long receiverUid, @Param("cursor") Long cursor, @Param("pageSize") Integer pageSize);

    Long countUnreadByReceiver(@Param("receiverUid") Long receiverUid);

    int markReceiverRead(@Param("id") Long id, @Param("receiverUid") Long receiverUid, @Param("readTime") LocalDateTime readTime);

    int decide(@Param("id") Long id,
               @Param("receiverUid") Long receiverUid,
               @Param("status") String status,
               @Param("chatAccessStatus") String chatAccessStatus,
               @Param("decidedAt") LocalDateTime decidedAt);

    int updateChatAccessStatus(@Param("id") Long id, @Param("chatAccessStatus") String chatAccessStatus);

    TutorApplication selectLatestAcceptedBetween(@Param("uidA") Long uidA, @Param("uidB") Long uidB);

    TutorApplication selectLatestPendingBetween(@Param("senderUid") Long senderUid, @Param("receiverUid") Long receiverUid);

    TutorApplication selectLatestBySenderReceiverContext(@Param("senderUid") Long senderUid,
                                                        @Param("receiverUid") Long receiverUid,
                                                        @Param("contextType") String contextType,
                                                        @Param("contextId") Long contextId);

    Long countCreatedBySenderBetween(@Param("senderUid") Long senderUid, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    int bindRoom(@Param("id") Long id, @Param("roomId") Long roomId);
}
