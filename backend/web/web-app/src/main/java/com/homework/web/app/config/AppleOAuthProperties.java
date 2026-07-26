package com.homework.web.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth.apple")
public class AppleOAuthProperties {
    private String clientId;
    private String teamId;
    private String keyId;
    private String privateKey;
    private String redirectUri;
    //Apple是用 teamId + keyId + privateKey 代替 clientSecret

    private String tokenUri;
    private String jwksUri;
}
