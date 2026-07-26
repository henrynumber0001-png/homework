package com.homework.web.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Trusted input produced only after a payment provider callback has passed
 * signature verification. It is deliberately not accepted by a public user API.
 */
@Data
public class MembershipPaymentConfirmationDTO {

    private String orderNo;

    private String providerTradeNo;

    private BigDecimal paidAmount;

    private String currency;

    private LocalDateTime paidTime;
}
