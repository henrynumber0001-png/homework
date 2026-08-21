package com.homework.web.admin.service;

import java.time.LocalDateTime;

/** 邀请事务提交后发送邮件所需的内存事件。 */
public record AdminInvitationCreatedEvent(
        Long invitationId,
        String email,
        String displayName,
        String rawToken,
        LocalDateTime expiresTime
) {
}
