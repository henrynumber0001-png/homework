package com.homework.web.admin.service;

import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionImageServiceTest {

    private COSClient cosClient;
    private CosReadUrlSigner readUrlSigner;
    private QuestionImageService service;

    @BeforeEach
    void setUp() {
        cosClient = mock(COSClient.class);
        readUrlSigner = mock(CosReadUrlSigner.class);
        when(readUrlSigner.sign(anyString())).thenReturn("https://cos.example.com/signed-question.png");

        TencentCosProperties properties = new TencentCosProperties();
        properties.setRegion("ap-singapore");
        properties.setSecretId("secret-id");
        properties.setSecretKey("secret-key");
        properties.setBucket("homework-1234567890");
        properties.setReadUrlTtlSeconds(3600);
        service = new QuestionImageService(cosClient, properties, readUrlSigner);
    }

    @Test
    void uploadStoresImageInTemporaryDirectory() {
        LocalDateTime beforeUpload = LocalDateTime.now();
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "question.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        var result = service.upload(image);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cosClient).putObject(requestCaptor.capture());
        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("homework-1234567890", request.getBucketName());
        assertEquals(result.getUploadId(), request.getKey());
        assertEquals("image/png", request.getMetadata().getContentType());
        assertEquals(3L, request.getMetadata().getContentLength());
        assertTrue(result.getUploadId().startsWith("admin-temp/questions/"));
        assertEquals("https://cos.example.com/signed-question.png", result.getPreviewUrl());
        assertTrue(result.getPreviewUrlExpiresTime().isAfter(beforeUpload.plusMinutes(59)));
        assertTrue(result.getPreviewUrlExpiresTime().isBefore(beforeUpload.plusMinutes(61)));
        assertTrue(result.getUploadExpiresTime().isAfter(beforeUpload.plusHours(23)));
        assertTrue(result.getUploadExpiresTime().isBefore(beforeUpload.plusHours(25)));
    }

    @Test
    void bindCopiesImageToPermanentDirectoryAndDeletesTemporaryObject() {
        String uploadId = "admin-temp/questions/2026-07-26/"
                + System.currentTimeMillis() + "-question.png";

        String result = service.bind(uploadId);

        ArgumentCaptor<CopyObjectRequest> requestCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(cosClient).copyObject(requestCaptor.capture());
        CopyObjectRequest request = requestCaptor.getValue();
        assertEquals("homework-1234567890", request.getSourceBucketName());
        assertEquals(uploadId, request.getSourceKey());
        assertEquals("homework-1234567890", request.getDestinationBucketName());
        assertEquals("questions/2026-07-26/" + uploadId.substring(uploadId.lastIndexOf('/') + 1),
                request.getDestinationKey());
        verify(cosClient).deleteObject("homework-1234567890", uploadId);
        assertEquals(request.getDestinationKey(), result);
    }

    @Test
    void uploadRejectsUnsupportedFileTypeBeforeCallingCos() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "question.txt",
                "text/plain",
                new byte[]{1}
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.homework.common.exception.HomeworkException.class,
                () -> service.upload(file)
        );
        org.mockito.Mockito.verify(cosClient, org.mockito.Mockito.never())
                .putObject(any(PutObjectRequest.class));
    }
}
