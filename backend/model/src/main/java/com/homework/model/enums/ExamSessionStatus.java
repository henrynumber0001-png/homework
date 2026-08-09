package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ExamSessionStatus implements BaseEnum {
    IN_PROGRESS(1, "in_progress"),
    SUBMITTED(2, "submitted"),
    EXPIRED(3, "expired");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String name;
    ExamSessionStatus(Integer code, String name) {
        this.code = code; this.name = name;
    }
}
