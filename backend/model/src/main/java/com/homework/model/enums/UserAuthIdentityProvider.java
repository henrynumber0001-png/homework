package com.homework.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserAuthIdentityProvider implements BaseEnum {

    EMAIL_PASSWORD(1, "email_password"),
    PHONE_OTP(2, "phone_otp"),
    GOOGLE(3, "google"),
    APPLE(4, "apple"),
    WECHAT(5, "wechat"),
    QQ(6, "qq");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserAuthIdentityProvider(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
