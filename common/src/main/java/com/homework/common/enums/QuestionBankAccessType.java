package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QuestionBankAccessType {

    FREE(1, "free"),
    PREMIUM(2, "premium"),
    MIXED_PREVIEW(3, "mixed_preview");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionBankAccessType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
