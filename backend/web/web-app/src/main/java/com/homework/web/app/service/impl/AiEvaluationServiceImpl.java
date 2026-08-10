package com.homework.web.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.dto.AiEvaluationResult;
import com.homework.web.app.service.AiEvaluationService;
import com.homework.web.app.service.LlmClient;
import com.homework.web.app.service.LlmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class AiEvaluationServiceImpl implements AiEvaluationService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiEvaluationResult evaluateInterviewAnswer(String title, String content, String analysis) {
        if(!StringUtils.hasText(title) || !StringUtils.hasText(analysis)){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //用户作答为空，不应该抛异常，否则用户收不到反馈了（而且既然允许提交空答案，就不应该抛异常）
        if (!StringUtils.hasText(content)) {
            AiEvaluationResult result = new AiEvaluationResult();
            result.setScoreRate(BigDecimal.ZERO);
            result.setAccurateComment("");
            result.setInnovativeComment("");
            result.setMissingComment("未提交有效答案。");
            result.setWrongComment("");
            result.setSummary("您并未有效作答，请参考下方的参考答案。");
            result.setModelName("rule-empty-answer"); //空答案的 来源标记，表示这次没有真的调用 AI，而是后端用固定规则生成的 0 分反馈，以后你查数据库时会很清楚
            return result;
        }

        String prompt = """
                你是一个面试题评分助手。请根据题目、参考答案和用户答案，对用户答案进行评分。

                评分要求：
                1. scoreRate 为 0 到 100 的数字
                2. accurateComment 写用户答得准确的地方
                3. missingComment 写用户遗漏的关键点
                4. wrongComment 写用户错误或不严谨的地方
                5. innovativeComment 写用户答案中有启发性的地方，没有则写空字符串
                6. summary 用一小段话总结改进建议

                只返回 JSON，不要返回 Markdown，不要解释，也不要增加其他字段。

                JSON 格式：
                {
                  "scoreRate": 0,
                  "accurateComment": "",
                  "innovativeComment": "",
                  "missingComment": "",
                  "wrongComment": "",
                  "summary": ""
                }

                题目：
                %s

                参考答案：
                %s

                用户答案：
                %s
                """.formatted(title, analysis, content);

        LlmResponse llmResponse = llmClient.chatJson(prompt);
        if (llmResponse == null || !StringUtils.hasText(llmResponse.content())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        try {
            //把llmResponse的 content 也就是Json字符串，反序列化为 AiEvaluationResult 对象
            //注意是把 llmResponse 中的 content，而不是把 llmResponse 反序列化
            AiEvaluationResult result = objectMapper.readerFor(AiEvaluationResult.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(llmResponse.content());
            validateRequiredFields(result);
            normalizeScore(result);
            // 模型名称来自厂商 HTTP 响应，不能由模型在评分 JSON 中自行声明。
            result.setModelName(llmResponse.modelName());
            return result;
        } catch (JsonProcessingException e) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR, e);
        }
    }

    private void validateRequiredFields(AiEvaluationResult result) {
        if (result == null
                || result.getScoreRate() == null
                || result.getAccurateComment() == null
                || result.getInnovativeComment() == null
                || result.getMissingComment() == null
                || result.getWrongComment() == null
                || result.getSummary() == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
    }

    private void normalizeScore(AiEvaluationResult result) {
        if (result.getScoreRate().compareTo(BigDecimal.ZERO) < 0) {
            result.setScoreRate(BigDecimal.ZERO);
        }

        if (result.getScoreRate().compareTo(BigDecimal.valueOf(100)) > 0) {
            result.setScoreRate(BigDecimal.valueOf(100));
        }
    }
}
