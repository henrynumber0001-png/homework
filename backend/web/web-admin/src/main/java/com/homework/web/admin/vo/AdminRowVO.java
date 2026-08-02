package com.homework.web.admin.vo;

import com.homework.model.enums.AdminRole;
import com.homework.model.enums.AdminStatus;
import com.homework.model.enums.BankDataScope;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 后台管理员列表行及完整访问配置。 */
@Data
public class AdminRowVO {

    /** 管理员 ID。 */
    private Long id;

    /** 登录邮箱。 */
    private String email;

    /** 展示名称。 */
    private String displayName;

    /** 角色名称。 */
    private AdminRole role;

    /** 账号状态名称。 */
    private AdminStatus status;

    /** 功能权限码。 */
    private List<String> permissions;

    /** 题库范围名称。 */
    private BankDataScope bankDataScope;

    /** 明确分配的题库 ID。 */
    private List<Long> assignedBankIds;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginTime;

    /** 乐观锁版本。 */
    private Integer version;
}
