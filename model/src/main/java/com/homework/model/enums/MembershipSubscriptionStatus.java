package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipSubscriptionStatus implements BaseEnum {

    ACTIVE(1, "active"),
    EXPIRED(2, "expired"),
    CANCELLED(3, "cancelled");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipSubscriptionStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
