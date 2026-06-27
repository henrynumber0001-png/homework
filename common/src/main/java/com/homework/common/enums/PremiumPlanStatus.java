package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumPlanStatus {

    ACTIVE(1, "active"),
    DISABLED(2, "disabled");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumPlanStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
