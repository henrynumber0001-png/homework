package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** Excel 题目导入任务状态。 */
@Getter
public enum QuestionImportStatus implements BaseEnum {

    VALIDATING(1, "validating"),
    READY(2, "ready"),
    INVALID(3, "invalid"),
    IMPORTING(4, "importing"),
    SUCCEEDED(5, "succeeded"),
    FAILED(6, "failed"),
    EXPIRED(7, "expired");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    QuestionImportStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
