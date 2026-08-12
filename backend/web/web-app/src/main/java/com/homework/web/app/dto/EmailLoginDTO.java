package com.homework.web.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailLoginDTO {

    @Schema(description = "登录邮箱，规范化后作为身份 account")
    private String email;
    private String password;
    private String turnstileToken;
}
