package com.homework.web.admin.dto;

import com.homework.model.enums.MembershipAction;
import com.homework.model.enums.MembershipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员发放、暂停、恢复或回收会员的请求。 */
@Data
public class MembershipActionDTO {

    /** 本次要对用户会员权益执行的固定动作。 */
    @NotNull
    private MembershipAction action;

    /** GRANT 动作要发放的会员类型；其他动作不使用。 */
    private MembershipType membershipType;

    /** GRANT 动作要发放的会员月数；其他动作不使用。 */
    private Integer durationMonths;

    /** 管理员执行本次会员操作的原因。 */
    @NotBlank
    @Size(max = 500)
    private String reason;

    /** 前端最近一次读取到的会员台账版本。 */
    @NotNull
    private Integer ledgerVersion;
}
