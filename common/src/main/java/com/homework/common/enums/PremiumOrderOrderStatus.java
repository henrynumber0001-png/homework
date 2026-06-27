package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumOrderOrderStatus {

    PENDING(1, "pending"),
    PAID(2, "paid"),
    CANCELLED(3, "cancelled"),
    EXPIRED(4, "expired"),
    REFUND(5, "refund");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumOrderOrderStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
