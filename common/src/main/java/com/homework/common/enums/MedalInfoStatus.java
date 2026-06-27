package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MedalInfoStatus {

    ACTIVE(1, "active"),
    DISABLED(2, "disabled");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    MedalInfoStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
