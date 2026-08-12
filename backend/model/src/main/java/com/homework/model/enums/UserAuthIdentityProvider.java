package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserAuthIdentityProvider implements BaseEnum {

    EMAIL_PASSWORD(1, "email_password"),
    GOOGLE(2, "google"),
    APPLE(3, "apple"),
    WECHAT(4, "wechat"),
    QQ(5, "qq");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String name;

    UserAuthIdentityProvider(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
