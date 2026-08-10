package com.homework.web.app.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * 这个配置类完成四件事：
 * 注册 LlmProperties。
 * 配置 Qwen Base URL。
 * 自动添加 Authorization: Bearer ...。
 * 设置连接和响应超时。
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {
    @Bean
    @Qualifier("qwenRestClient")
    @ConditionalOnProperty(name = "llm.provider", havingValue = "qwen")
    public RestClient qwenRestClient(LlmProperties properties) {
        validateQwenProperties(properties);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(properties.getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.getReadTimeout().toMillis()));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private void validateQwenProperties(LlmProperties properties) {
        Assert.hasText(properties.getBaseUrl(), "LLM_BASE_URL 未配置");
        Assert.hasText(properties.getApiKey(), "LLM_API_KEY 未配置");
        Assert.hasText(properties.getModel(), "LLM_MODEL 未配置");
    }
}
