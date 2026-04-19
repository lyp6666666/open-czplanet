package com.ai.tutor.appointment.job;

import com.ai.tutor.appointment.service.InviteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 邀请返利月结任务。
 *
 * <p>默认每天凌晨检查一次，仅当当天等于 Nacos 配置的 settlementDay 时才生成上月结算单。</p>
 */
@Slf4j
@Component
public class InviteSettlementJob {

    @Resource
    private InviteService inviteService;

    @Scheduled(cron = "${invite.settlement-cron:0 15 2 * * ?}")
    public void generateMonthlySettlements() {
        int count = inviteService.generateMonthlySettlements(LocalDate.now());
        if (count > 0) {
            log.info("invite_settlement_generated count={}", count);
        }
    }
}
