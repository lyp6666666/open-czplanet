package com.ai.tutor.videocallimservice.chat.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InfoFeeCalculatorTest {

    @Test
    void shouldUseAveragePriceForDemandBudgetRange() {
        Long hourlyPriceFen = InfoFeeCalculator.resolveDemandHourlyPriceFen(new BigDecimal("300"), new BigDecimal("500"));

        long amountFen = InfoFeeCalculator.computeFromHourlyPriceFen(hourlyPriceFen, 3, 2);

        assertThat(hourlyPriceFen).isEqualTo(40000L);
        assertThat(amountFen).isEqualTo(192000L);
    }

    @Test
    void shouldUseSinglePriceWhenOnlyOneDemandBudgetExists() {
        Long hourlyPriceFen = InfoFeeCalculator.resolveDemandHourlyPriceFen(new BigDecimal("400"), null);

        long amountFen = InfoFeeCalculator.computeFromHourlyPriceFen(hourlyPriceFen, 4, 2);

        assertThat(hourlyPriceFen).isEqualTo(40000L);
        assertThat(amountFen).isEqualTo(224000L);
    }

    @Test
    void shouldUseAveragePriceForProposalRangeText() {
        Long hourlyPriceFen = InfoFeeCalculator.resolveProposalHourlyPriceFen("300-500元/小时");

        long amountFen = InfoFeeCalculator.computeFromHourlyPriceFen(hourlyPriceFen, 6, 2);

        assertThat(hourlyPriceFen).isEqualTo(40000L);
        assertThat(amountFen).isEqualTo(240000L);
    }

    @Test
    void shouldUseSinglePriceForProposalSingleValueText() {
        Long hourlyPriceFen = InfoFeeCalculator.resolveProposalHourlyPriceFen("450元/小时");

        long amountFen = InfoFeeCalculator.computeFromHourlyPriceFen(hourlyPriceFen, 2, 2);

        assertThat(hourlyPriceFen).isEqualTo(45000L);
        assertThat(amountFen).isEqualTo(162000L);
    }

    @Test
    void shouldTreatFrequencyBelowOneAsOncePerWeek() {
        long amountFen = InfoFeeCalculator.computeFromHourlyPriceFen(30000L, 0, 2);

        assertThat(amountFen).isEqualTo(60000L);
    }
}
