package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.admin.config.MinioProperties;
import com.homework.web.admin.vo.QuestionImageUploadVO;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 使用 MinIO 保存和解析题目图片。 */
@Service
@RequiredArgsConstructor
public class QuestionImageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public QuestionImageUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE
                || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        };
        String objectName = "admin-temp/questions/%s/%s.%s".formatted(
                LocalDate.now(),
                System.currentTimeMillis() + "-"
                        + UUID.randomUUID().toString().toLowerCase(Locale.ROOT),
                extension
        );
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }

        QuestionImageUploadVO result = new QuestionImageUploadVO();
        result.setUploadId(objectName);
        result.setUrl(toPublicUrl(objectName));
        result.setExpiresTime(LocalDateTime.now().plusHours(24));
        return result;
    }

    public String resolveUrl(String uploadId) {
        if (uploadId == null || uploadId.isBlank()) {
            return null;
        }
        if (!uploadId.startsWith("admin-temp/questions/") || uploadId.contains("..")) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        String fileName = uploadId.substring(uploadId.lastIndexOf('/') + 1);
        int separator = fileName.indexOf('-');
        try {
            long uploadedAt = Long.parseLong(fileName.substring(0, separator));
            long expiresAt = uploadedAt + java.time.Duration.ofHours(24).toMillis();
            if (System.currentTimeMillis() > expiresAt) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
        } catch (HomeworkException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR, exception);
        }
        return toPublicUrl(uploadId);
    }

    public String bind(String uploadId) {
        if (uploadId == null || uploadId.isBlank()) {
            return null;
        }
        resolveUrl(uploadId);
        String targetObjectName = uploadId.replaceFirst("^admin-temp/questions/", "questions/");
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(targetObjectName)
                    .source(CopySource.builder()
                            .bucket(properties.getBucket())
                            .object(uploadId)
                            .build())
                    .build());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(uploadId)
                    .build());
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
        return toPublicUrl(targetObjectName);
    }

    private String toPublicUrl(String objectName) {
        String baseUrl = properties.getPublicBaseUrl().endsWith("/")
                ? properties.getPublicBaseUrl().substring(0, properties.getPublicBaseUrl().length() - 1)
                : properties.getPublicBaseUrl();
        return baseUrl + "/" + objectName;
    }
}
