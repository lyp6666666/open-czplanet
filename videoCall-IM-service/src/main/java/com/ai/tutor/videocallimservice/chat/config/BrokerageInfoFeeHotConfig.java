package com.ai.tutor.videocallimservice.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 信息费（brokerage / brokerage_order.amount_fen）统一定价热更新配置。
 *
 * <p>用途：</p>
 * <ul>
 *   <li>当 {@code brokerage.info-fee.unified.enabled=true} 时，系统所有“信息费订单”创建时金额将强制使用统一值
 *       {@code brokerage.info-fee.unified.amount-fen}（单位：分）。</li>
 *   <li>当 {@code enabled=false} 时，恢复业务规则计算：按每周频次对应比例收取一周课时费，
 *       其中价格区间取上下限均值，单值取原值；缺失则回退到 {@code brokerage.amount-fen}。</li>
 * </ul>
 *
 * <p>生效范围（创建订单时读取）：</p>
 * <ul>
 *   <li>家教申请被同意后生成的信息费订单（tutor_application → brokerage_order）</li>
 *   <li>合作提案达成后生成的信息费订单（collaboration_proposal → brokerage_order）</li>
 * </ul>
 *
 * <p>注意：</p>
 * <ul>
 *   <li>该配置只影响“新创建”的订单，不会回写历史订单金额。</li>
 *   <li>需要 Nacos/Config 启用 refresh（本服务 application.yml 已配置 refreshEnabled=true）。</li>
 * </ul>
 *
 * <p>Nacos 建议配置：</p>
 * <ul>
 *   <li>DataId: {@code videoCall-IM-service.yaml} 或 {@code videoCall-IM-service-${spring.profiles.active}.yaml}</li>
 *   <li>Group: {@code DEFAULT_GROUP}</li>
 * </ul>
 *
 * <pre>
 * brokerage:
 *   info-fee:
 *     unified:
 *       enabled: true      # true=开启统一信息费；false=关闭（走按频次阶梯的一周课时费规则）
 *       amount-fen: 9900   # 统一信息费金额，单位：分，必须 &gt; 0；例如 99 元 = 9900
 * </pre>
 */
@Component
@RefreshScope
public class BrokerageInfoFeeHotConfig {

    /**
     * 统一信息费开关。
     *
     * <p>为 true 时，信息费订单创建时一律使用 {@link #unifiedAmountFen}。</p>
     * <p>默认 false：保持现有业务规则不变。</p>
     */
    @Value("${brokerage.info-fee.unified.enabled:false}")
    private boolean unifiedEnabled;

    /**
     * 统一信息费金额（单位：分）。
     *
     * <p>仅当 {@link #unifiedEnabled} 为 true 时生效。</p>
     * <p>默认 19900（199 元）。</p>
     */
    @Value("${brokerage.info-fee.unified.amount-fen:19900}")
    private long unifiedAmountFen;

    public boolean isUnifiedEnabled() {
        return unifiedEnabled;
    }

    public long getUnifiedAmountFen() {
        return unifiedAmountFen;
    }
}
