package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserQuestionNoteVisibility {

    PRIVATE_VALUE(1, "private"),
    PUBLIC_VALUE(2, "public");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserQuestionNoteVisibility(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
