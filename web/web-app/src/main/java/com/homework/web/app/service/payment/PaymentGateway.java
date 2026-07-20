package com.homework.web.app.service.payment;

import com.homework.model.enums.MembershipOrderPayType;
import com.homework.web.app.vo.PaymentPayloadVO;

/**
 * 支付渠道适配器。会员业务只依赖该接口，不直接依赖微信 SDK。
 */
public interface PaymentGateway {

    MembershipOrderPayType payType();

    PaymentPayloadVO prepay(PaymentPrepayRequest request);

    /**
     * 对已到本地支付截止时间的订单执行“查单；未支付则关单”。
     */
    PaymentReconciliationResult reconcileExpiredOrder(String orderNo);
}
