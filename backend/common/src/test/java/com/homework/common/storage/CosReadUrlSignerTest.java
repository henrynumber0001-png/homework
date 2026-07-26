package com.homework.common.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosReadUrlSignerTest {

    @Test
    void signGeneratesOneHourGetUrlForObjectKey() throws Exception {
        COSClient cosClient = mock(COSClient.class);
        ArgumentCaptor<Date> expiresCaptor = ArgumentCaptor.forClass(Date.class);
        when(cosClient.generatePresignedUrl(
                org.mockito.ArgumentMatchers.eq("homework-1234567890"),
                org.mockito.ArgumentMatchers.eq("questions/test.png"),
                expiresCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(HttpMethodName.GET)
        )).thenReturn(URI.create("https://cos.example.com/questions/test.png?signed=true").toURL());
        CosReadUrlSigner signer = new CosReadUrlSigner(cosClient, "homework-1234567890", 3600);

        Instant beforeSign = Instant.now();
        String result = signer.sign("questions/test.png");

        assertEquals("https://cos.example.com/questions/test.png?signed=true", result);
        assertTrue(expiresCaptor.getValue().toInstant().isAfter(beforeSign.plusSeconds(3590)));
        assertTrue(expiresCaptor.getValue().toInstant().isBefore(beforeSign.plusSeconds(3610)));
    }

    @Test
    void signReturnsNullWithoutCallingCosForBlankObjectKey() {
        COSClient cosClient = mock(COSClient.class);
        CosReadUrlSigner signer = new CosReadUrlSigner(cosClient, "homework-1234567890", 3600);

        assertNull(signer.sign(null));
        assertNull(signer.sign(" "));
        verify(cosClient, never()).generatePresignedUrl(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Date.class),
                org.mockito.ArgumentMatchers.any(HttpMethodName.class)
        );
    }
}
