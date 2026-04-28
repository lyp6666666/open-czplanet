package com.ai.tutor.videocallimservice.chat.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InfoFeeCalculator {

    private static final Pattern PRICE_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private InfoFeeCalculator() {
    }

    static long computeFromHourlyPriceFen(Long hourlyPriceFen, Integer frequencyPerWeek, int lessonHours) {
        if (hourlyPriceFen == null || hourlyPriceFen <= 0L) {
            return 0L;
        }
        long lessonsPerWeek = normalizeFrequency(frequencyPerWeek);
        BigDecimal weeklyCourseFeeFen = BigDecimal.valueOf(hourlyPriceFen)
                .multiply(BigDecimal.valueOf(lessonHours))
                .multiply(BigDecimal.valueOf(lessonsPerWeek));
        BigDecimal infoFeeFen = weeklyCourseFeeFen.multiply(resolveInfoFeeRate(frequencyPerWeek))
                .setScale(0, RoundingMode.CEILING);
        long amountFen = infoFeeFen.longValue();
        return Math.max(1L, amountFen);
    }

    static Long resolveDemandHourlyPriceFen(BigDecimal budgetMin, BigDecimal budgetMax) {
        return resolveAveragePriceFen(budgetMin, budgetMax);
    }

    static Long resolveProposalHourlyPriceFen(String priceText) {
        String raw = priceText == null ? "" : priceText.trim();
        if (raw.isEmpty()) {
            return null;
        }
        BigDecimal first = null;
        BigDecimal second = null;
        Matcher matcher = PRICE_NUMBER.matcher(raw);
        while (matcher.find()) {
            BigDecimal value;
            try {
                value = new BigDecimal(matcher.group(1));
            } catch (Exception ignored) {
                continue;
            }
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (first == null) {
                first = value;
            } else {
                second = value;
                break;
            }
        }
        return resolveAveragePriceFen(first, second);
    }

    static BigDecimal resolveInfoFeeRate(Integer frequencyPerWeek) {
        long frequency = normalizeFrequency(frequencyPerWeek);
        if (frequency <= 1) {
            return BigDecimal.ONE;
        }
        if (frequency == 2) {
            return new BigDecimal("0.9");
        }
        if (frequency == 3) {
            return new BigDecimal("0.8");
        }
        if (frequency == 4) {
            return new BigDecimal("0.7");
        }
        if (frequency == 5) {
            return new BigDecimal("0.6");
        }
        return new BigDecimal("0.5");
    }

    private static long normalizeFrequency(Integer frequencyPerWeek) {
        return frequencyPerWeek == null || frequencyPerWeek <= 0 ? 1L : frequencyPerWeek.longValue();
    }

    private static Long resolveAveragePriceFen(BigDecimal first, BigDecimal second) {
        BigDecimal left = normalizePositive(first);
        BigDecimal right = normalizePositive(second);
        if (left == null && right == null) {
            return null;
        }
        BigDecimal value = left;
        if (left == null) {
            value = right;
        } else if (right != null) {
            value = left.add(right).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }
        return value.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private static BigDecimal normalizePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value;
    }
}
