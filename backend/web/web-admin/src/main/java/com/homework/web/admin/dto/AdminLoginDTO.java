package com.homework.web.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理员登录请求。 */
@Data
public class AdminLoginDTO {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String turnstileToken;
}
