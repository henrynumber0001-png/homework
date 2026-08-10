package com.homework.web.app.service;

/**
 * 大模型厂商响应的统一表示。
 *
 * @param content 模型生成的正文；结构化请求时，该字段是 JSON 字符串
 * @param modelName 厂商实际返回的模型名称
 * @param requestId 厂商请求 ID，便于排查问题
 * @param inputTokens 本次请求消耗的输入 Token 数
 * @param outputTokens 本次请求消耗的输出 Token 数
 */

//让 LLM返回的消息对象、token消耗 统一转化成固定的格式，这样 返回的信息丰富，且是封装的，调用起来也方便。
//比如，QuestionInfoServiceImpl中的第1102和1103行，把 MessageContent 和 ModelName，统一处理了（一个返回值，直接调用里面的 content和modelName 成员变量的值，很方便）
//LLM 入参出参就是只读数据快照：创建之后只读取，不修改，需要 equals/hashCode/toString，还需要参数校验
//record 刚好把这些能力内置，少写大量模板代码，编译期保证不可变，减少 bug。
public record LlmResponse(
        String content,
        String modelName,
        String requestId,
        Integer inputTokens,
        Integer outputTokens
) {
}
