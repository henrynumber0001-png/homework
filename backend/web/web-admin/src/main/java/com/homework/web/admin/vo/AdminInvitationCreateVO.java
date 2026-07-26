package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员邀请创建结果。 */
@Data
public class AdminInvitationCreateVO {

    /** 被邀请邮箱。 */
    private String email;

    /** 可直接复制给被邀请人的邀请链接。 */
    private String invitationUrl;

    /** 邀请到期时间。 */
    private LocalDateTime expiresTime;
}
