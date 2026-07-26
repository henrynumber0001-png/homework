package com.homework.web.app.service.impl;

import com.homework.web.app.service.LlmClient;
import org.springframework.stereotype.Service;

@Service
public class MockLlmClient implements LlmClient {

    @Override
    public String chat(String prompt) {
        return """
                {
                  "scoreRate": 60,
                  "accurateComment": "基本回答了题目核心概念，能看出已经理解主要知识点。",
                  "innovativeComment": "",
                  "missingComment": "答案还可以补充更多关键细节，例如应用场景、边界条件或与相关概念的区别。",
                  "wrongComment": "",
                  "summary": "这是 Mock AI 反馈：你的答案有一定基础，但还需要围绕参考答案补充关键点，让表达更完整、更像面试中的高质量回答。",
                  "modelName": "mock-llm"
                }
                """;
    }
}
