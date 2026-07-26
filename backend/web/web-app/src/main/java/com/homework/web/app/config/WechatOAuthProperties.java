package com.homework.web.app.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth.wechat")
public class WechatOAuthProperties {

    private String appId;

    private String appSecret;

    private String redirectUri;

    private String scope;

    private String statePrefix;

    private String authorizeUrl;

    private String accessTokenUrl;

    private String userInfoUrl;

    private String lang;

}
