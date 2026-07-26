package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MembershipStatus implements BaseEnum {

    FREE(0, "free"),
    PREMIUM(1, "premium"),
    PREMIUM_PLUS(2, "premium_plus");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MembershipStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
