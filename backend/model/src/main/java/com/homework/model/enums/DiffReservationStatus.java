package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DiffReservationStatus implements BaseEnum{

    RESERVED(1, "reserved"),
    CONSUMED(2, "consumed"),
    RELEASED(3, "released");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    DiffReservationStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
