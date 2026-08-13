package com.homework.web.admin.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ses.v20201002.SesClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TencentSesProperties.class)
public class TencentSesConfig {

    @Bean
    public SesClient sesClient(TencentSesProperties properties) {
        Credential credential = new Credential(
                properties.getSecretId(),
                properties.getSecretKey()
        );

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ses.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        return new SesClient(
                credential,
                properties.getRegion(),
                clientProfile
        );
    }
}
