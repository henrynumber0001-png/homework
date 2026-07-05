package com.homework.web.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.dto.AiEvaluationResult;
import com.homework.web.app.service.AiEvaluationService;
import com.homework.web.app.service.LlmClient;
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

                只返回 JSON，不要返回 Markdown，不要解释。

                JSON 格式：
                {
                  "scoreRate": 0,
                  "accurateComment": "",
                  "innovativeComment": "",
                  "missingComment": "",
                  "wrongComment": "",
                  "summary": "",
                  "modelName": ""
                }

                题目：
                %s

                参考答案：
                %s

                用户答案：
                %s
                """.formatted(title, analysis, content);

        String aiResponse = llmClient.chat(prompt);

        try {
            AiEvaluationResult result = objectMapper.readValue(aiResponse, AiEvaluationResult.class);
            normalizeScore(result);
            return result;
        } catch (JsonProcessingException e) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
    }

    private void normalizeScore(AiEvaluationResult result) {
        if (result.getScoreRate() == null) {
            result.setScoreRate(BigDecimal.ZERO);
        }

        if (result.getScoreRate().compareTo(BigDecimal.ZERO) < 0) {
            result.setScoreRate(BigDecimal.ZERO);
        }

        if (result.getScoreRate().compareTo(BigDecimal.valueOf(100)) > 0) {
            result.setScoreRate(BigDecimal.valueOf(100));
        }
    }
}
