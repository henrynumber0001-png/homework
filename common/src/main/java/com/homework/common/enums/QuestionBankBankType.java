package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QuestionBankBankType {

    INTERVIEW(1, "interview"),
    CERTIFICATION(2, "certification");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionBankBankType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
