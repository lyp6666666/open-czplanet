package com.ai.tutor.payment.service;

import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDateTime;

/**
 * 支付订单服务接口
 */
public interface PaymentOrderService extends IService<PaymentOrder> {

    /**
     * 根据订单号查询订单
     * @param orderNo 商户订单号
     * @return 订单信息
     */
    PaymentOrder getByOrderNo(String orderNo);

    /**
     * 更新订单状态为成功
     * @param orderNo 商户订单号
     * @param transactionId 第三方交易号
     * @param successTime 支付成功时间
     * @return 是否更新成功
     */
    boolean updateSuccess(String orderNo, String transactionId, LocalDateTime successTime);

    /**
     * 更新订单状态为失败
     * @param orderNo 商户订单号
     * @param reason 失败原因
     * @return 是否更新成功
     */
    boolean updateFailed(String orderNo, String reason);

    /**
     * 统一下单：创建或复用一笔待支付订单（同业务单+同渠道幂等）。
     *
     * @param contextType 业务上下文类型
     * @param contextId   业务上下文ID
     * @param userId      付款用户ID
     * @param channel     支付渠道
     * @param amount      金额（分）
     * @param subject     标题
     * @param body        描述
     * @param clientIp    客户端IP
     * @return 待支付订单
     */
    PaymentOrder createOrReusePending(String contextType, Long contextId, Long userId, String channel, Long amount, String subject, String body, String clientIp);

    /**
     * 更新支付要素信息（二维码/支付链接）与过期时间。
     *
     * @param orderNo 商户订单号
     * @param payData 支付要素（JSON）
     * @param expireTime 过期时间
     * @return 是否更新成功
     */
    boolean updatePayData(String orderNo, String payData, LocalDateTime expireTime);

    /**
     * 记录回调接收信息（计数、时间、验签结果）。
     *
     * @param orderNo 商户订单号
     * @param notifyVerified 验签是否通过：0否 1是
     * @return 是否更新成功
     */
    boolean recordNotifyReceipt(String orderNo, int notifyVerified);

    /**
     * 回调驱动的支付成功更新：同时写入第三方交易信息与回调验签信息（幂等）。
     *
     * @param orderNo 商户订单号
     * @param transactionId 第三方交易号
     * @param providerOrderNo 第三方系统单号
     * @param successTime 支付成功时间
     * @param notifyVerified 验签是否通过：0否 1是
     * @return 是否更新成功或已是成功
     */
    boolean updateSuccessFromNotify(String orderNo, String transactionId, String providerOrderNo, LocalDateTime successTime, int notifyVerified);

    /**
     * 标记支付成功事件已投递成功。
     *
     * @param orderNo 商户订单号
     * @return 是否更新成功
     */
    boolean markEventSent(String orderNo);

    /**
     * 标记支付成功事件投递失败原因（用于排障与后续重试）。
     *
     * @param orderNo 商户订单号
     * @param reason 失败原因（建议截断到合理长度）
     * @return 是否更新成功
     */
    boolean markEventSendFailed(String orderNo, String reason);

    /**
     * 按业务上下文查询最近一笔支付成功的支付单。
     *
     * <p>用于退款等反向链路定位原支付单。</p>
     *
     * @param contextType 业务上下文类型
     * @param contextId   业务上下文ID
     * @return 最近一笔成功支付单；不存在返回 null
     */
    PaymentOrder getLatestSuccessByContext(String contextType, Long contextId);
}
