package com.homework.web.admin.vo;

import lombok.Data;

/** 管理员基础信息。 */
@Data
public class AdminSummaryVO {

    /** 管理员 ID。 */
    private Long id;

    /** 登录邮箱。 */
    private String email;

    /** 后台显示名称。 */
    private String displayName;

    /** 管理员角色。 */
    private String role;

    /** 管理员状态。 */
    private String status;
}
