package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GroupType implements BaseEnum {

    INTERVIEW(1, "interview"),
    CERTIFICATION(2, "certification");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    GroupType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
