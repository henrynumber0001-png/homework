package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 高风险操作二次认证请求。 */
@Data
public class AdminReauthDTO {

    @NotBlank
    private String password;

    @NotBlank
    private String actionScope;
}
