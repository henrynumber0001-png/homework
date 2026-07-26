package com.homework.web.admin.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** 创建管理端共用的 MinIO 客户端。 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient.Builder builder = MinioClient.builder().endpoint(properties.getEndpoint());
        if (StringUtils.hasText(properties.getAccessKey())
                && StringUtils.hasText(properties.getSecretKey())) {
            builder.credentials(properties.getAccessKey(), properties.getSecretKey());
        }
        return builder.build();
    }
}
