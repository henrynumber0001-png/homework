package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AiChatMessage;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.enums.GroupType;
import com.homework.web.app.dto.AiFollowUpDTO;
import com.homework.web.app.mapper.CertificateQuestionInfoMapper;
import com.homework.web.app.mapper.InterviewQuestionInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 提示词构造器。
 * 这个类只负责两件事：
 * 1. 根据题库类型查询当前题目的题目和解析，构造“题目上下文”。
 * 2. 把题目上下文、历史对话、用户最新追问拼成最终 prompt。
 */
@Component
@RequiredArgsConstructor
public class AiPromptBuilder {

    private final InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;

    /**
     * 构造当前追问所属题目的上下文。
     * 面试题和认证题存放在不同表里，所以这里根据 bankType 分开查询。
     */
    public String buildQuestionContext(AiFollowUpDTO dto) {
        if (dto.getBankType() == GroupType.INTERVIEW) {
            return buildInterviewQuestionContext(dto);
        }
        if (dto.getBankType() == GroupType.CERTIFICATION) {
            return buildCertificateQuestionContext(dto);
        }
        throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
    }

    /**
     * 构造最终发给大模型的 prompt。
     * history 是当前 AI 会话中已经发生过的历史消息；currentQuestion 是用户这次刚输入的问题。
     */
    public String buildAiChatPrompt(
            String questionContext,
            List<AiChatMessage> history,
            String currentQuestion
    ) {
        String historyText = history == null || history.isEmpty()
                ? "暂无历史对话。"
                : history.stream()
                .map(message -> message.getSenderType().getLabel() + "：" + message.getMessageContent())
                .collect(Collectors.joining("\n"));

        return """
                你是一个帮助用户理解题目答案解析的 AI 助手。
                请只围绕当前题目的知识点、答案解析、用户追问进行回答。
                回答要清晰、具体，适合学习场景；如果用户问题含糊，先基于题目解析给出最可能有帮助的解释。

                当前题目上下文：
                %s

                历史对话：
                %s

                用户最新问题：
                %s
                """.formatted(questionContext, historyText, currentQuestion);
    }

    private String buildInterviewQuestionContext(AiFollowUpDTO dto) {
        InterviewQuestionInfo question = interviewQuestionInfoMapper.selectOne(
                new LambdaQueryWrapper<InterviewQuestionInfo>()
                        .eq(InterviewQuestionInfo::getId, dto.getQuestionId())
                        .eq(InterviewQuestionInfo::getIsReleased, true)
        );

        if (question == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        return """
                题库类型：面试题库
                题目：%s
                参考解析：%s
                """.formatted(question.getTitle(), question.getAnalysis());
    }

    private String buildCertificateQuestionContext(AiFollowUpDTO dto) {
        CertificateQuestionInfo question = certificateQuestionInfoMapper.selectOne(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .eq(CertificateQuestionInfo::getId, dto.getQuestionId())
                        .eq(CertificateQuestionInfo::getBankId, dto.getBankId())
                        .eq(CertificateQuestionInfo::getIsReleased, true)
        );

        if (question == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        return """
                题库类型：认证题库
                题目：%s
                正确答案：%s
                答案解析：%s
                """.formatted(
                question.getTitle(),
                question.getCorrectAnswer(),
                question.getAnalysis()
        );
    }
}
