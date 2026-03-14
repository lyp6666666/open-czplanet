package com.ai.tutor.common.integration;

/**
 * 中介费订单领域对外的最小能力门面。
 *
 * <p>目标：支付域仅依赖该门面读取“可支付订单信息”，避免直接依赖业务域内部的 Service/Mapper，
 * 为后续拆分（HTTP/RPC 调用）预留演进空间。</p>
 */
public interface BrokerageOrderFacade {

    /**
     * 获取一笔“可发起支付”的中介费订单信息（用于统一下单前的校验与取数）。
     *
     * @param brokerageOrderId 中介费订单 id
     * @param uid              当前用户 id（用于校验是否为付款人）
     * @return 可支付订单信息
     */
    BrokerageOrderPayInfo getPayableOrder(Long brokerageOrderId, Long uid);
}

