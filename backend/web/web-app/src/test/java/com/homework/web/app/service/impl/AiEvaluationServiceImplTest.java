package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.dto.AiEvaluationResult;
import com.homework.web.app.service.LlmClient;
import com.homework.web.app.service.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEvaluationServiceImplTest {

    private LlmClient llmClient;
    private AiEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        service = new AiEvaluationServiceImpl(llmClient, new ObjectMapper());
    }

    @Test
    void evaluatesStructuredJsonAndUsesProviderModelName() {
        String json = """
                {
                  "scoreRate": 88,
                  "accurateComment": "核心概念正确",
                  "innovativeComment": "",
                  "missingComment": "缺少边界条件",
                  "wrongComment": "",
                  "summary": "补充边界条件后会更完整"
                }
                """;
        when(llmClient.chatJson(contains("只返回 JSON")))
                .thenReturn(new LlmResponse(json, "qwen-plus", "request-1", 100, 50));

        AiEvaluationResult result = service.evaluateInterviewAnswer(
                "什么是线程安全？",
                "多个线程同时执行不会出现错误。",
                "线程安全要求共享状态在并发访问下保持正确。"
        );

        assertEquals(new BigDecimal("88"), result.getScoreRate());
        assertEquals("核心概念正确", result.getAccurateComment());
        assertEquals("qwen-plus", result.getModelName());
    }

    @Test
    void rejectsJsonMissingRequiredFields() {
        when(llmClient.chatJson(contains("只返回 JSON")))
                .thenReturn(new LlmResponse(
                        "{\"scoreRate\":88,\"summary\":\"字段不完整\"}",
                        "qwen-plus",
                        "request-2",
                        null,
                        null
                ));

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.evaluateInterviewAnswer("题目", "回答", "解析")
        );

        assertEquals(ResultCodeEnum.DATA_ERROR, exception.getResultCodeEnum());
    }

    @Test
    void rejectsUnexpectedJsonFields() {
        String json = """
                {
                  "scoreRate": 88,
                  "accurateComment": "正确",
                  "innovativeComment": "",
                  "missingComment": "",
                  "wrongComment": "",
                  "summary": "总结",
                  "unexpected": "不允许的字段"
                }
                """;
        when(llmClient.chatJson(contains("只返回 JSON")))
                .thenReturn(new LlmResponse(json, "qwen-plus", "request-3", null, null));

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.evaluateInterviewAnswer("题目", "回答", "解析")
        );

        assertEquals(ResultCodeEnum.DATA_ERROR, exception.getResultCodeEnum());
    }

    @Test
    void emptyAnswerUsesRuleResultWithoutCallingLlm() {
        AiEvaluationResult result = service.evaluateInterviewAnswer("题目", "  ", "解析");

        assertEquals(BigDecimal.ZERO, result.getScoreRate());
        assertEquals("rule-empty-answer", result.getModelName());
        verify(llmClient, never()).chatJson(org.mockito.ArgumentMatchers.anyString());
    }
}
