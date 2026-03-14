package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminPaymentOrderMapper;
import com.ai.tutor.admin.model.entity.PaymentOrderRecord;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminPaymentRecordService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AdminPaymentRecordServiceImpl implements AdminPaymentRecordService {

    @Resource
    private AdminPaymentOrderMapper adminPaymentOrderMapper;

    @Override
    public PageResult<PaymentOrderRecord> list(int page, int size, String orderNo, Long userId, String contextType, Long contextId, String channel, String status, LocalDateTime startTime, LocalDateTime endTime) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 200);
        long offset = (long) (p - 1) * s;

        List<PaymentOrderRecord> records = adminPaymentOrderMapper.list(
                offset,
                s,
                trim(orderNo),
                userId,
                trim(contextType),
                contextId,
                trim(channel),
                trim(status),
                startTime,
                endTime
        );
        long total = adminPaymentOrderMapper.count(
                trim(orderNo),
                userId,
                trim(contextType),
                contextId,
                trim(channel),
                trim(status),
                startTime,
                endTime
        );

        return PageResult.<PaymentOrderRecord>builder()
                .records(records)
                .total(total)
                .size(s)
                .current(p)
                .build();
    }

    @Override
    public PaymentOrderRecord detail(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PaymentOrderRecord record = adminPaymentOrderMapper.getByOrderNo(orderNo.trim());
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return record;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}

