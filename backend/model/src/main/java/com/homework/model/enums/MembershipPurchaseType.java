package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipPurchaseType implements BaseEnum {

    FULL(1, "full"),
    DIFF(2, "diff");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipPurchaseType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
