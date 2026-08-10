package com.homework.web.app.service.impl;

import com.homework.web.app.config.LlmProperties;
import com.homework.web.app.service.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenLlmClientTest {

    private static final String BASE_URL = "https://example.cn-beijing.maas.aliyuncs.com/compatible-mode/v1";

    private MockRestServiceServer server;
    private QwenLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();

        LlmProperties properties = new LlmProperties();
        properties.setModel("qwen-test");
        client = new QwenLlmClient(builder.build(), properties);
    }

    @Test
    void chatOmitsJsonResponseFormatAndMapsProviderMetadata() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model", is("qwen-test")))
                .andExpect(jsonPath("$.messages[0].role", is("user")))
                .andExpect(jsonPath("$.messages[0].content", is("解释这道题")))
                .andExpect(jsonPath("$.response_format").doesNotExist())
                .andRespond(withSuccess(successResponse("普通回复"), MediaType.APPLICATION_JSON));

        LlmResponse response = client.chat("解释这道题");

        assertEquals("普通回复", response.content());
        assertEquals("qwen-test", response.modelName());
        assertEquals("chatcmpl-test", response.requestId());
        assertEquals(12, response.inputTokens());
        assertEquals(8, response.outputTokens());
        server.verify();
    }

    @Test
    void chatJsonRequestsJsonObjectResponseFormat() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.response_format.type", is("json_object")))
                .andRespond(withSuccess(successResponse("{\"scoreRate\":88}"), MediaType.APPLICATION_JSON));

        LlmResponse response = client.chatJson("请返回 JSON");

        assertEquals("{\"scoreRate\":88}", response.content());
        assertEquals("qwen-test", response.modelName());
        server.verify();
    }

    @Test
    void mockClientSeparatesTextAndJsonResponses() {
        MockLlmClient mockClient = new MockLlmClient();

        LlmResponse textResponse = mockClient.chat("普通问题");
        LlmResponse jsonResponse = mockClient.chatJson("评分问题");

        assertEquals("mock-llm", textResponse.modelName());
        assertEquals("mock-llm", jsonResponse.modelName());
        assertNull(textResponse.requestId());
        assertTrue(jsonResponse.content().contains("\"scoreRate\""));
    }

    private String successResponse(String content) {
        return """
                {
                  "id": "chatcmpl-test",
                  "model": "qwen-test",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": %s
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 8,
                    "total_tokens": 20
                  }
                }
                """.formatted(quoteJson(content));
    }

    private String quoteJson(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
