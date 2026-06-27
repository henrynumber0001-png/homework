package com.homework.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserAuthIdentityProvider {

    EMAIL_PASSWORD(1, "email_password"),
    PHONE_OTP(2, "phone_otp"),
    GOOGLE(3, "google"),
    WECHAT(4, "wechat"),
    QQ(5, "qq");

    @EnumValue
    @JsonValue
    private final Integer value;

    private final String label;

    UserAuthIdentityProvider(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
