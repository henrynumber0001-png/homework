package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ExamAttemptStatus implements BaseEnum {
    IN_PROGRESS(1, "in_progress"),
    SUBMITTED(2, "submitted"),
    EXPIRED(3, "expired"),
    ABANDONED(4, "abandoned");

    @EnumValue
    @JsonValue
    private final Integer value;
    private final String label;
    ExamAttemptStatus(Integer value, String label) {
        this.value = value; this.label = label;
    }
}
