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
    private final Integer code;

    private final String name;

    CertificateMode(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
