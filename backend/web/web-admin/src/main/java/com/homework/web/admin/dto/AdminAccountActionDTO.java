package com.homework.web.admin.dto;

import com.homework.model.enums.AdminAccountAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 停用、启用或归档普通管理员账号的请求。 */
@Data
public class AdminAccountActionDTO {

    /** 本次要对普通管理员账号执行的固定状态动作。 */
    @NotNull
    private AdminAccountAction action;

    /** 超级管理员执行本次账号操作的原因。 */
    @NotBlank
    @Size(max = 500)
    private String reason;

    /** 前端最近一次读取到的管理员账号乐观锁版本。 */
    @NotNull
    private Integer version;
}
