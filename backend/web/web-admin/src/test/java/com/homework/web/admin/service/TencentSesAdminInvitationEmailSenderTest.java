package com.homework.web.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.web.admin.config.TencentSesProperties;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TencentSesAdminInvitationEmailSenderTest {

    @Test
    void sendsOnlyRawTokenToApprovedSesTemplate() throws Exception {
        SesClient sesClient = mock(SesClient.class);
        TencentSesProperties properties = new TencentSesProperties();
        properties.setTemplateId(123L);
        properties.setFromEmail("Homework <noreply@ithomework.online>");
        ObjectMapper objectMapper = new ObjectMapper();
        TencentSesAdminInvitationEmailSender sender =
                new TencentSesAdminInvitationEmailSender(sesClient, properties, objectMapper);
        String rawToken = "96f47b5c-c497-4b7a-a91b-c18752d64045";

        sender.sendInvitation(
                "admin@example.com",
                "Admin",
                rawToken,
                LocalDateTime.of(2026, 8, 19, 12, 0)
        );

        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).SendEmail(requestCaptor.capture());
        String templateData = requestCaptor.getValue().getTemplate().getTemplateData();
        JsonNode data = objectMapper.readTree(templateData);

        assertEquals(rawToken, data.get("rowToken").asText());
        assertFalse(data.get("rowToken").asText().contains("https://"));
        assertFalse(data.get("rowToken").asText().contains("?token="));
    }
}
