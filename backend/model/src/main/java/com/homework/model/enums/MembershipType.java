package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipType implements BaseEnum {

    PREMIUM(1, "premium"),
    PREMIUM_PLUS(2, "premium_plus");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
