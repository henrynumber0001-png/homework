package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 高风险操作二次认证结果。 */
@Data
public class AdminReauthVO {

    /** 一次性二次认证令牌。 */
    private String reauthToken;

    /** 令牌过期时间。 */
    private LocalDateTime expiresTime;
}
