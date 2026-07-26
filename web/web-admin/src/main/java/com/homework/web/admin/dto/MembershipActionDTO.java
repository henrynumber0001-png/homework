package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员发放、暂停、恢复或回收会员的请求。 */
@Data
public class MembershipActionDTO {

    @NotBlank
    private String action;

    private String membershipType;

    private Integer durationMonths;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull
    private Integer ledgerVersion;
}
