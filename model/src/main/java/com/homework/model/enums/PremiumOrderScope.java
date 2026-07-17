package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PremiumOrderScope implements BaseEnum {

    INTERVIEW(1, "interview"),
    CERTIFICATION(2, "certification"),
    FULLACCESS(3, "all");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PremiumOrderScope(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
