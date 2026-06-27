package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PracticeSessionQuestionStatus {

    NOT_ANSWERED(1, "not_answered"),
    ANSWERED(2, "answered"),
    CORRECT(3, "correct"),
    WRONG(4, "wrong"),
    MARKED(5, "marked");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PracticeSessionQuestionStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
