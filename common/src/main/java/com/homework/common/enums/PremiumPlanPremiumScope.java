package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumPlanPremiumScope {

    INTERVIEW(1, "interview"),
    CERTIFICATION(2, "certification");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumPlanPremiumScope(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
