package com.homework.web.app.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员模块传给支付网关的预下单参数。
 * 金额、币种和截止时间全部来自服务端订单，不能使用前端传价。
 */
public record PaymentPrepayRequest(
        String orderNo,
        BigDecimal amount,
        String currency,
        LocalDateTime expiresAt,
        String description
) {
}
