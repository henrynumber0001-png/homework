package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 私信聊天盒的发送权限状态。 */
@Getter
public enum PrivateChatAccess implements BaseEnum {
    PENDING_REPLY(1, "pending_reply"),
    OPEN(2, "open");

    @EnumValue
    @JsonValue
    private final Integer value;
    private final String label;

    PrivateChatAccess(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
