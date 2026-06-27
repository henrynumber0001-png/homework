package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumPlanBillingCycle {

    MONTHLY(1, "monthly"),
    YEARLY(2, "yearly");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumPlanBillingCycle(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
