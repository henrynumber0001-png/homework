package com.homework.web.admin.service;

import java.time.LocalDateTime;

public interface EmailInvitationSender {
    void sendInvitation(
            String email,
            String displayName,
            String rawToken,
            LocalDateTime expiresTime
    );
}
