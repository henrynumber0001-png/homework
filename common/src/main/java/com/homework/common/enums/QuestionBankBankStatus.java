package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QuestionBankBankStatus {

    DRAFT(1, "draft"),
    PUBLISHED(2, "published"),
    ARCHIVED(3, "archived");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionBankBankStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
