package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipOrderStatus implements BaseEnum {

    PENDING(1, "pending"),           // 等待支付
    PAID(2, "paid"),                 // 已支付
    CANCELLED(3, "cancelled"),       // 用户或系统取消
    EXPIRED(4, "payment_expired"),   // 支付超时，不是会员权益到期
    REFUNDED(5, "refunded"),
    PAY_FAILED(6, "pay_failed");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipOrderStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
