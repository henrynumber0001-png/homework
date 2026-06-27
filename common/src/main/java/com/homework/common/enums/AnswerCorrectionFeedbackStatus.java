package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AnswerCorrectionFeedbackStatus {

    PENDING(1, "pending"),
    ACCEPTED(2, "accepted"),
    REJECTED(3, "rejected"),
    RESOLVED(4, "resolved");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    AnswerCorrectionFeedbackStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
