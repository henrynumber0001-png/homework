package com.homework.web.app.service.payment;

import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;

/**
 * 支付平台查单结果。只有 PAID 状态携带经过平台签名响应确认的支付信息。
 */
public record PaymentReconciliationResult(
        PaymentReconciliationStatus status,
        MembershipPaymentConfirmationDTO confirmation
) {

    public static PaymentReconciliationResult paid(
            MembershipPaymentConfirmationDTO confirmation
    ) {
        return new PaymentReconciliationResult(
                PaymentReconciliationStatus.PAID,
                confirmation
        );
    }

    public static PaymentReconciliationResult closed() {
        return new PaymentReconciliationResult(
                PaymentReconciliationStatus.CLOSED,
                null
        );
    }

    public static PaymentReconciliationResult pending() {
        return new PaymentReconciliationResult(
                PaymentReconciliationStatus.PENDING,
                null
        );
    }
}
