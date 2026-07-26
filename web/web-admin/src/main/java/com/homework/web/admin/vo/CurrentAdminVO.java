package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 当前管理员、权限和会话信息。 */
@Data
public class CurrentAdminVO {

    /** 当前管理员基础信息。 */
    private AdminSummaryVO admin;

    /** 功能权限码。 */
    private List<String> permissions;

    /** 题库数据范围。 */
    private String bankDataScope;

    /** 被明确分配的题库 ID。 */
    private List<Long> assignedBankIds;

    /** 当前会话到期时间。 */
    private LocalDateTime sessionExpiresTime;
}
