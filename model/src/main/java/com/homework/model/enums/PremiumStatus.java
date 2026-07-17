package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumStatus implements BaseEnum {

    ACTIVE(1, "active"),
    EXPIRED(2, "expired"),
    DISABLED(3, "disabled");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
