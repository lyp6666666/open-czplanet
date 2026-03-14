package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.entity.PaymentOrderRecord;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminPaymentRecordService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/payment/orders")
@Tag(name = "Admin Payment Records", description = "付款记录查询与排障")
public class AdminPaymentRecordController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private AdminPaymentRecordService adminPaymentRecordService;

    @GetMapping
    @Operation(summary = "分页查询付款记录")
    public BaseResponse<PageResult<PaymentOrderRecord>> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                                             @RequestParam(value = "size", defaultValue = "10") int size,
                                                             @RequestParam(value = "orderNo", required = false) String orderNo,
                                                             @RequestParam(value = "userId", required = false) Long userId,
                                                             @RequestParam(value = "contextType", required = false) String contextType,
                                                             @RequestParam(value = "contextId", required = false) Long contextId,
                                                             @RequestParam(value = "channel", required = false) String channel,
                                                             @RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "startTime", required = false) String startTime,
                                                             @RequestParam(value = "endTime", required = false) String endTime) {
        return ResultUtils.success(adminPaymentRecordService.list(
                page,
                size,
                orderNo,
                userId,
                contextType,
                contextId,
                channel,
                status,
                parseDateTime(startTime),
                parseDateTime(endTime)
        ));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "付款记录详情")
    public BaseResponse<PaymentOrderRecord> detail(@PathVariable("orderNo") String orderNo) {
        return ResultUtils.success(adminPaymentRecordService.detail(orderNo));
    }

    private static LocalDateTime parseDateTime(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.trim(), DT);
        } catch (Exception e) {
            return null;
        }
    }
}

