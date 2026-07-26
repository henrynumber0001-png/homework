package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员邀请公开预览。 */
@Data
public class AdminInvitationPreviewVO {

    /** 脱敏后的邀请邮箱。 */
    private String emailMasked;

    /** 被邀请人的显示名称。 */
    private String displayName;

    /** 邀请过期时间。 */
    private LocalDateTime expiresTime;

    /** 邀请当前是否可接受。 */
    private Boolean valid;
}
