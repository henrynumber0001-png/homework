package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 接受管理员邀请并设置密码的请求。 */
@Data
public class AdminInvitationAcceptDTO {

    @NotBlank
    @Size(min = 12, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;
}
