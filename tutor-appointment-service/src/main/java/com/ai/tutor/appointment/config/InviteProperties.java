package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邀请有礼业务配置。
 *
 * <p>该配置以 tutor-appointment-service 为主读取来源，支持从 Nacos 动态下发。
 * 当前主要用于页面规则展示与返利基数计算，后续月结算任务也可复用本配置。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "invite")
public class InviteProperties {

    /**
     * 是否开启邀请功能总开关。
     */
    private boolean enabled = true;

    /**
     * 是否展示前端入口。
     */
    private boolean entryEnabled = true;

    /**
     * 邀请教师返利比例。
     */
    private double teacherRewardRate = 0.13D;

    /**
     * 邀请学生返利比例。
     */
    private double studentRewardRate = 0.13D;

    /**
     * 月度结算日。
     */
    private int settlementDay = 10;

    /**
     * 最低结算门槛，单位分。
     */
    private long minSettlementAmountFen = 1000L;

    /**
     * 退款后是否冻结返利。
     */
    private boolean freezeOnRefund = true;

    /**
     * 是否启用邀请返利月结任务。
     */
    private boolean settlementJobEnabled = true;

    /**
     * 月结任务 Cron，默认每天凌晨 02:15 检查一次，实际仅在 settlementDay 当天执行。
     */
    private String settlementCron = "0 15 2 * * ?";

    /**
     * 收款信息提示文案。
     */
    private String receiverHint = "请确保微信收款信息真实有效，平台将在每月结算日统一打款。";

    /**
     * 系统邀请码默认码值。
     */
    private String systemInviteCode = "CHUANGZHI";

    /**
     * 系统邀请码默认链接，为空时由服务端按前端域名兜底生成。
     */
    private String systemInviteLink = "";

    /**
     * 系统邀请码默认推广标题。
     */
    private String systemPromoTitle = "创智推广专属福利";

    /**
     * 系统邀请码默认推广说明。
     */
    private String systemPromoDesc = "使用创智推广码注册后，教师信息费享受推广期减半，学生可按教师实付信息费获得返现。";

    /**
     * 系统邀请码教师信息费折扣，0.5 表示半价。
     */
    private double systemTutorInfoFeeDiscountRate = 0.5D;

    /**
     * 系统邀请码学生返现比例。
     */
    private double systemStudentRewardRate = 0.13D;

    /**
     * 默认前端站点地址，用于生成系统邀请码分享链接。
     */
    private String webOrigin = "http://localhost:5173";
}
