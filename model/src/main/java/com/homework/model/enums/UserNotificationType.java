package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserNotificationType implements BaseEnum {

    COMMENT(1, "comment"),
    LIKE(2, "like"),
    SYSTEM(3, "system"),
    PRIVATE_MESSAGE(4, "private_message"),
    FAVORITE(5, "favorite"),
    REPOST(6, "repost"),
    FOLLOW(7, "follow"),
    MENTION(8, "mention");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserNotificationType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
