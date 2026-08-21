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
        String rawToken = "96f47b5c-c497-4b7a-a91b-c18752d64045";
        AdminInvitationCreatedEvent event = new AdminInvitationCreatedEvent(
                10L,
                "admin@example.com",
                "Admin",
                rawToken,
                expiresTime
        );

        listener.sendInvitation(event);

        verify(sender).sendInvitation(
                "admin@example.com",
                "Admin",
                rawToken,
                expiresTime
        );
    }

    @Test
    void containsSenderFailureBecauseInvitationIsAlreadyCommitted() {
        EmailInvitationSender sender = mock(EmailInvitationSender.class);
        AdminInvitationEmailListener listener = new AdminInvitationEmailListener(sender);
        String rawToken = "96f47b5c-c497-4b7a-a91b-c18752d64045";
        AdminInvitationCreatedEvent event = new AdminInvitationCreatedEvent(
                10L,
                "admin@example.com",
                "Admin",
                rawToken,
                LocalDateTime.now().plusHours(24)
        );
        doThrow(new RuntimeException("SES unavailable"))
                .when(sender)
                .sendInvitation(
                        event.email(),
                        event.displayName(),
                        event.rawToken(),
                        event.expiresTime()
                );

        assertDoesNotThrow(() -> listener.sendInvitation(event));
    }
}
