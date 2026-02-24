package com.ai.tutor.videocallimservice.chat.mapper;

import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface BrokerageOrderMapper {
    void insert(BrokerageOrder order);

    BrokerageOrder selectById(@Param("id") Long id);

    BrokerageOrder selectByProposalId(@Param("proposalId") Long proposalId);

    int submitProof(@Param("id") Long id,
                    @Param("payMethod") String payMethod,
                    @Param("proofUrl") String proofUrl,
                    @Param("proofNote") String proofNote);

    int markPaid(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt);
}
