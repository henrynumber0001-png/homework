package com.homework.web.app.service;

public interface LlmClient {

    /**
     * 生成普通文本回复。
     */
    LlmResponse chat(String prompt);

    /**
     * 生成 JSON 格式回复。具体业务结构仍需由调用方反序列化并校验。
     */
    LlmResponse chatJson(String prompt);
}
