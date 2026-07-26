package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 当前管理员修改密码的请求。 */
@Data
public class AdminPasswordChangeDTO {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 72)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
