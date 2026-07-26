package com.homework.web.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Admin Token 签发配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "admin.jwt")
public class AdminJwtProperties {

    private String issuer;
    private String secretKey;
    private long ttlSeconds;
}
