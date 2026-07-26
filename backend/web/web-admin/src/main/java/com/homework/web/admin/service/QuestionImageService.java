package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.homework.web.admin.vo.QuestionImageUploadVO;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 使用腾讯云 COS 保存和解析题目图片。 */
@Service
@RequiredArgsConstructor
public class QuestionImageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final COSClient cosClient;
    private final TencentCosProperties properties;
    private final CosReadUrlSigner readUrlSigner;

    /** 将题目图片上传到24小时有效的临时目录。 */
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
        String previewUrl;
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            cosClient.putObject(new PutObjectRequest(
                    properties.getBucket(),
                    objectName,
                    inputStream,
                    metadata
            ));
            previewUrl = readUrlSigner.sign(objectName);
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }

        LocalDateTime now = LocalDateTime.now();
        QuestionImageUploadVO result = new QuestionImageUploadVO();
        result.setUploadId(objectName);
        result.setPreviewUrl(previewUrl);
        result.setPreviewUrlExpiresTime(now.plusSeconds(properties.getReadUrlTtlSeconds()));
        result.setUploadExpiresTime(now.plusHours(24));
        return result;
    }

    /** 将临时图片复制到正式目录、删除原对象，并返回正式对象 Key。 */
    public String bind(String uploadId) {
        if (uploadId == null || uploadId.isBlank()) {
            return null;
        }
        validateUploadId(uploadId);
        String targetObjectName = uploadId.replaceFirst("^admin-temp/questions/", "questions/");
        try {
            cosClient.copyObject(new CopyObjectRequest(
                    properties.getBucket(),
                    uploadId,
                    properties.getBucket(),
                    targetObjectName
            ));
            cosClient.deleteObject(properties.getBucket(), uploadId);
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
        return targetObjectName;
    }

    /** 校验临时图片标识的目录和24小时有效期。 */
    public void validateUploadId(String uploadId) {
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
    }
}
