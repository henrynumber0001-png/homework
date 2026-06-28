package com.homework.web.app.dto;

import com.homework.common.enums.UserAuthIdentityProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailRegisterDTO {


    @Schema(description = "注册类型对应的注册id,这里是邮箱")
    private String email;

    private String password;

    private String passwordConfirm;

    @Schema(description = "nickName")
    private String displayName;

    @Schema(description = "turnstile token")
    private String turnstileToken;
}
