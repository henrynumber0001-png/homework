package com.homework.web.admin.vo;

import lombok.Data;

import java.util.List;

/** 管理员登录成功结果。 */
@Data
public class AdminLoginVO {

    /** Admin JWT。 */
    private String accessToken;

    /** 固定为 Bearer。 */
    private String tokenType;

    /** Token 剩余有效秒数。 */
    private Long expiresInSeconds;

    /** 当前管理员基础信息。 */
    private AdminSummaryVO admin;

    /** 当前管理员功能权限码。 */
    private List<String> permissions;

    /** 当前管理员题库数据范围。 */
    private String bankDataScope;
}
