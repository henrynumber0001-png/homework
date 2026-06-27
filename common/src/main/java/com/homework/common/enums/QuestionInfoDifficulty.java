package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QuestionInfoDifficulty {

    EASY(1, "easy"),
    MEDIUM(2, "medium"),
    HARD(3, "hard");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionInfoDifficulty(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
