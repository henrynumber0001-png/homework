package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 题目的草稿、发布、下架和删除状态。 */
@Getter
public enum QuestionInfoStatus implements BaseEnum {

    DRAFT(1, "draft"),
    PUBLISHED(2, "published"),
    OFFLINE(3, "offline"),
    DELETED(4, "deleted");

    /** 保存到数据库并通过接口返回的状态值。 */
    @EnumValue
    @JsonValue
    private final Integer value;

    /** 状态的英文说明。 */
    private final String label;

    QuestionInfoStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
