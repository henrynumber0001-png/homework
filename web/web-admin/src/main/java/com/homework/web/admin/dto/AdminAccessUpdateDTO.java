package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 修改普通管理员功能权限和题库范围的请求。 */
@Data
public class AdminAccessUpdateDTO {

    @NotEmpty
    private List<String> permissions;

    @NotNull
    private String bankDataScope;

    private List<Long> assignedBankIds;

    @NotNull
    private Integer version;

    @Size(max = 500)
    private String reason;
}
