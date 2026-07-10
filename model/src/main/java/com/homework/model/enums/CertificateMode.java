package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CertificateMode implements BaseEnum {

    PRACTICE(1, "practice"),
    EXAM(2, "exam");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    CertificateMode(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
