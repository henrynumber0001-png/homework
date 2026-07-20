package com.homework.web.app.service.payment;

/**
 * 服务端主动向支付平台查单后的归一化结果。
 */
public enum PaymentReconciliationStatus {
    PAID,
    CLOSED,
    PENDING
}
