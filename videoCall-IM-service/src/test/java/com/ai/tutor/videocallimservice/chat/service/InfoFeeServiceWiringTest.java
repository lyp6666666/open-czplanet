package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.StudentJobPostingLite;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.mapper.StudentJobPostingLiteMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfoFeeServiceWiringTest {

    @Test
    void tutorApplicationServiceShouldUseDemandBudgetAverageAndFrequencyLadder() {
        StudentJobPostingLiteMapper demandMapper = mock(StudentJobPostingLiteMapper.class);
        when(demandMapper.selectById(99L)).thenReturn(StudentJobPostingLite.builder()
                .id(99L)
                .frequencyPerWeek(6)
                .budgetMin(new BigDecimal("200"))
                .budgetMax(new BigDecimal("300"))
                .build());

        TutorApplicationService service = new TutorApplicationService();
        ReflectionTestUtils.setField(service, "studentJobPostingLiteMapper", demandMapper);
        ReflectionTestUtils.setField(service, "defaultAmountFen", 19900L);

        TutorApplication application = TutorApplication.builder()
                .contextType("DEMAND")
                .contextId(99L)
                .build();

        long amountFen = ReflectionTestUtils.invokeMethod(service, "computeInfoFeeAmountFenForApplication", application);

        assertThat(amountFen).isEqualTo(150000L);
    }

    @Test
    void brokerageOrderServiceShouldUseProposalPriceAverageAndFrequencyLadder() {
        BrokerageOrderService service = new BrokerageOrderService();
        ReflectionTestUtils.setField(service, "defaultAmountFen", 19900L);

        CollaborationProposal proposal = CollaborationProposal.builder()
                .frequencyPerWeek(5)
                .pricePerHour("300-500元/小时")
                .build();

        long amountFen = ReflectionTestUtils.invokeMethod(service, "computeInfoFeeAmountFen", proposal);

        assertThat(amountFen).isEqualTo(240000L);
    }
}
