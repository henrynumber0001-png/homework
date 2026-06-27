package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QuestionInfoQuestionType {

    SINGLE_CHOICE(1, "single_choice"),
    MULTIPLE(2, "multiple"),
    TRUE_FALSE(3, "true_false"),
    SHORT_ANSWER(4, "short_answer"),
    ESSAY(5, "essay");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionInfoQuestionType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
