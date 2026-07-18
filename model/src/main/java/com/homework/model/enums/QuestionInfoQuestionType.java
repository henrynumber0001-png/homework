package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QuestionInfoQuestionType implements BaseEnum {

    SINGLE_CHOICE(1, "single_choice"),
    MULTIPLE(2, "multiple"),
    ESSAY(3, "essay");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionInfoQuestionType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
