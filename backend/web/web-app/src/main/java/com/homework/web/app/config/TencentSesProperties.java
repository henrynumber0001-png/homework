package com.homework.web.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "tencent-ses")
public class TencentSesProperties {

    @NotBlank
    private String secretId;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String region;

    @NotBlank
    private String fromEmail;

    @NotNull
    private Long templateId;
}
