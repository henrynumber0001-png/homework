package com.homework.web.app.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties { //负责把 yaml文件中的 LLM配置文件 自动转换成 Java 配置对象。

    private String provider = "mock";

    //例如：https://{workspace-id}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
    private String baseUrl;

    //从 LLM_API_KEY 环境变量读取。
    private String apiKey;

    private String model;

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(60);
}
