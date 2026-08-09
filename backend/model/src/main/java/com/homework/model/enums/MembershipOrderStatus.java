package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipOrderStatus implements BaseEnum {

    PENDING(1, "pending"),           // 等待支付
    PAID(2, "paid"),                 // 已支付
    EXPIRED(4, "payment_expired"),   // 支付超时，不是会员权益到期
    PAY_FAILED(6, "pay_failed");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    MembershipOrderStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
