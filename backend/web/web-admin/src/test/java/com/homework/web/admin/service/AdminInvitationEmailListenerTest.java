package com.homework.web.admin.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminInvitationEmailListenerTest {

    @Test
    void delegatesCommittedInvitationToEmailSender() {
        EmailInvitationSender sender = mock(EmailInvitationSender.class);
        AdminInvitationEmailListener listener = new AdminInvitationEmailListener(sender);
        LocalDateTime expiresTime = LocalDateTime.now().plusHours(24);
        AdminInvitationCreatedEvent event = new AdminInvitationCreatedEvent(
                10L,
                "admin@example.com",
                "Admin",
                "https://admin.example.com/admin/invitation?token=secret",
                expiresTime
        );

        listener.sendInvitation(event);

        verify(sender).sendInvitation(
                "admin@example.com",
                "Admin",
                "https://admin.example.com/admin/invitation?token=secret",
                expiresTime
        );
    }

    @Test
    void containsSenderFailureBecauseInvitationIsAlreadyCommitted() {
        EmailInvitationSender sender = mock(EmailInvitationSender.class);
        AdminInvitationEmailListener listener = new AdminInvitationEmailListener(sender);
        AdminInvitationCreatedEvent event = new AdminInvitationCreatedEvent(
                10L,
                "admin@example.com",
                "Admin",
                "https://admin.example.com/admin/invitation?token=secret",
                LocalDateTime.now().plusHours(24)
        );
        doThrow(new RuntimeException("SES unavailable"))
                .when(sender)
                .sendInvitation(
                        event.email(),
                        event.displayName(),
                        event.invitationUrl(),
                        event.expiresTime()
                );

        assertDoesNotThrow(() -> listener.sendInvitation(event));
    }
}
