package com.homework.web.admin.dto;

import com.homework.model.enums.UserAccountAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 停用、启用、封禁或解封 App 用户账号的请求。 */
@Data
public class UserAccountActionDTO {

    /** 本次要对 App 用户账号执行的固定状态动作。 */
    @NotNull
    private UserAccountAction action;

    /** 管理员执行本次账号操作的原因。 */
    @NotBlank
    @Size(max = 500)
    private String reason;

    /** 前端最近一次读取到的用户账号乐观锁版本。 */
    @NotNull
    private Integer version;
}
