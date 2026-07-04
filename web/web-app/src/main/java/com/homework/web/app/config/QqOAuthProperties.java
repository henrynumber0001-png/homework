package com.homework.web.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth.qq")
public class QqOAuthProperties {

    private String appId;

    private String appKey;

    private String redirectUri;

    private String authorizeUrl;

    private String tokenUrl;

    private String openidUrl;

    private String userInfoUrl;

    private String scope;

}
