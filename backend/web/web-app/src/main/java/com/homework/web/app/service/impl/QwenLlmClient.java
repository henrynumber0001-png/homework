package com.homework.web.app.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.config.LlmProperties;
import com.homework.web.app.service.LlmClient;
import com.homework.web.app.service.LlmResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@ConditionalOnProperty(
        name = "llm.provider",
        havingValue = "qwen"
)
public class QwenLlmClient implements LlmClient {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    private final RestClient restClient;
    private final LlmProperties properties;

    public QwenLlmClient(@Qualifier("qwenRestClient") RestClient restClient, LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public LlmResponse chat(String prompt) {
        return execute(prompt, null);
    }

    @Override
    public LlmResponse chatJson(String prompt) {
        return execute(prompt, new QwenResponseFormat("json_object"));
    }

    /**
     * 普通文本和 JSON 请求共用同一套 HTTP 调用逻辑，避免两种调用方式出现配置漂移。
     */
    private LlmResponse execute(String prompt, QwenResponseFormat responseFormat) {
        if (!StringUtils.hasText(prompt)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        QwenChatRequest request = new QwenChatRequest(
                properties.getModel(),
                List.of(new QwenRequestMessage("user", prompt)),
                DEFAULT_TEMPERATURE,
                false,
                responseFormat
        );

        try {
            QwenChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(QwenChatResponse.class);

            return extractResponse(response);
        } catch (RestClientException exception) {
            // 不要在这里输出 API Key 或完整请求头。
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR, exception);
        }
    }

    private LlmResponse extractResponse(QwenChatResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 项目当前实际按 Java 17 release 编译，因此不能使用 List#getFirst()。
        QwenChoice firstChoice = response.choices().get(0);
        if (firstChoice.message() == null || !StringUtils.hasText(firstChoice.message().content())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        QwenUsage usage = response.usage();
        return new LlmResponse(
                firstChoice.message().content(),
                response.model(),
                response.id(),
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens()
        );
    }

    /**
     * OpenAI-compatible 请求。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record QwenChatRequest(String model, List<QwenRequestMessage> messages,
                                   Double temperature, Boolean stream, @JsonProperty("response_format") QwenResponseFormat responseFormat) { }

    private record QwenResponseFormat(String type) { }

    private record QwenRequestMessage(String role, String content) { }

    /**
     * 只声明项目需要的响应字段。
     * 阿里云返回的其他字段由 Jackson 自动忽略。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenChatResponse(String id, String model, List<QwenChoice> choices, QwenUsage usage) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenChoice(Integer index, QwenResponseMessage message, @JsonProperty("finish_reason") String finishReason) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenResponseMessage(String role, String content) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}
