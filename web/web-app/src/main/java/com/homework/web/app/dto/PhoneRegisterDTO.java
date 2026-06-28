package com.homework.web.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PhoneRegisterDTO {

    @Schema(description = "注册类型对应的注册id,这里是手机验证码")
    private String identifier;

    @Schema(description = "短信验证码")
    private String smsCode;

    @Schema(description = "nickName")
    private String displayName;

}
