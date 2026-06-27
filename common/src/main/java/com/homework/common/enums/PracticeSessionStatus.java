package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PracticeSessionStatus {

    IN_PROGRESS(1, "in_progress"),
    SUBMITTED(2, "submitted"),
    COMPLETED(3, "completed"),
    ABANDONED(4, "abandoned");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PracticeSessionStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
