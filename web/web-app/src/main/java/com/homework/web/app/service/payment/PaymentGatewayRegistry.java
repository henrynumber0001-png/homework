package com.homework.web.app.service.payment;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.enums.MembershipOrderPayType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 根据订单 payType 选择支付渠道。
 *
 * <p>微信支付未启用或用户选择尚未实现的支付宝时，会在创建本地订单之前拒绝，
 * 避免生成永远无法付款的 PENDING 订单。
 */
@Component
public class PaymentGatewayRegistry {

    private final Map<MembershipOrderPayType, PaymentGateway> gateways =
            new EnumMap<>(MembershipOrderPayType.class);

    public PaymentGatewayRegistry(List<PaymentGateway> paymentGateways) {
        for (PaymentGateway gateway : paymentGateways) {
            PaymentGateway duplicate = gateways.put(gateway.payType(), gateway);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "Duplicate payment gateway: " + gateway.payType()
                );
            }
        }
    }

    public PaymentGateway require(MembershipOrderPayType payType) {
        PaymentGateway gateway = gateways.get(payType);
        if (gateway == null) {
            throw new HomeworkException(
                    ResultCodeEnum.MEMBERSHIP_PAYMENT_CHANNEL_UNAVAILABLE
            );
        }
        return gateway;
    }
}
