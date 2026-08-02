package com.homework.web.admin.dto;

import com.homework.model.enums.BankDataScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 超级管理员创建普通管理员邀请的请求。 */
@Data
public class AdminInvitationCreateDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @NotEmpty
    private List<String> permissions;

    @NotNull
    private BankDataScope bankDataScope;

    private List<Long> assignedBankIds;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
