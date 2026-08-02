package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 题库在后台管理中的发布状态。 */
@Getter
public enum QuestionBankStatus implements BaseEnum {

    DRAFT(1, "draft"),
    PUBLISHED(2, "published"),
    OFFLINE(3, "offline"),
    DELETED(4, "deleted");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    QuestionBankStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
