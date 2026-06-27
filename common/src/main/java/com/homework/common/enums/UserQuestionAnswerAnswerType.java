package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserQuestionAnswerAnswerType {

    TEXT(1, "text"),
    SINGLE_CHOICE(2, "single_choice"),
    MULTIPLE_CHOICE(3, "multiple_choice");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserQuestionAnswerAnswerType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
