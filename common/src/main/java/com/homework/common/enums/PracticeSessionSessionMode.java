package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PracticeSessionSessionMode {

    INTERVIEW_PRACTICE(1, "interview_practice"),
    CERT_PRACTICE(2, "cert_practice"),
    CERT_EXAM(3, "cert_exam");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    PracticeSessionSessionMode(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
