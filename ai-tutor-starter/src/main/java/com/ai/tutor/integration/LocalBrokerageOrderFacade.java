package com.ai.tutor.integration;

import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 中介费订单门面的本地实现（单体阶段）
 *
 * <p>说明：支付域通过 common 的 Facade 接口读取业务订单信息，避免直接依赖业务域内部实现。</p>
 */
@Component
@RequiredArgsConstructor
public class LocalBrokerageOrderFacade implements BrokerageOrderFacade {

    private final BrokerageOrderMapper brokerageOrderMapper;

    @Override
    public BrokerageOrderPayInfo getPayableOrder(Long brokerageOrderId, Long uid) {
        if (brokerageOrderId == null || uid == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        BrokerageOrder order = brokerageOrderMapper.selectById(brokerageOrderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!uid.equals(order.getPayerUid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (BrokerageOrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单已支付");
        }
        if (BrokerageOrderStatus.CANCELED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单已取消");
        }
        if (!BrokerageOrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单当前状态不可支付");
        }

        BrokerageOrderPayInfo info = new BrokerageOrderPayInfo();
        info.setOrderId(order.getId());
        info.setPayerUid(order.getPayerUid());
        info.setAmountFen(order.getAmountFen());
        info.setStatus(order.getStatus());
        info.setSubject("信息费支付");
        info.setBody("对接咨询费支付");
        info.setApplicationId(order.getApplicationId());
        return info;
    }
}
