package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserNotificationNotificationType {

    REPLY(1, "reply"),
    LIKE(2, "like"),
    SYSTEM(3, "system"),
    PRIVATE_MESSAGE(4, "private_message"),
    FAVORITE(5, "favorite"),
    REPOST(6, "repost"),
    FOLLOW(7, "follow");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserNotificationNotificationType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
