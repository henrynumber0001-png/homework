package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.assist.ISqlRunner;
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

@Component
@RequiredArgsConstructor
public class AiPromptBuilder {

    private final InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;

    //返回的是 当前用户停留题目的题目内容 + 答案解析（字符串）
    public String buildQuestionContext(AiFollowUpDTO dto) {
        if (dto.getGroupType() == GroupType.INTERVIEW) {
            return buildInterviewQuestionContext(dto);
        }
        if (dto.getGroupType() == GroupType.CERTIFICATION) {
            return buildCertificateQuestionContext(dto);
        }
        throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
    }

    /**
     * 构造最终发给大模型的 prompt。
     * history 是当前 AI 会话中已经发生过的历史消息；currentQuestion 是用户这次刚输入的问题。
     */
    public String buildAiChatPrompt(
            String questionContext, //题目和答案解析
            List<AiChatMessage> history, //这个session内的历史收发信息
            String currentQuestion //本次用户输入的问题
    ) {
        //List<AiChatMessage>
        //把每一条历史消息，全部序列化成一个JSON列表，然后作为prompt的一部分，喂给AI（好处就是结构清晰，AI易懂）
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

    //返回的是 当前用户停留题目的题目内容 + 答案解析（字符串）
    private String buildInterviewQuestionContext(AiFollowUpDTO dto) {
        // 变更：关系表已删除，AI上下文查询直接用题目实体的 bank_id + id 校验归属。
        InterviewQuestionInfo question = interviewQuestionInfoMapper.selectOne(
                new LambdaQueryWrapper<InterviewQuestionInfo>()
                        .eq(InterviewQuestionInfo::getId, dto.getQuestionId())
                        .eq(InterviewQuestionInfo::getBankId, dto.getBankId())
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
        /*
        这里return的，其实就是question.getTitle() 和 question.getAnalysis()
        为什么要格式化？
        因为这个字符串不是给前端看的，也不是普通业务字段，而是给 AI 模型看的 prompt 上下文。

        带标签有几个好处：
        1）AI 知道哪一段是题目，哪一段是参考答案，否则标题和解析拼在一起，模型可能混淆。
        2）AI 知道当前题库类型，面试题和认证题回答方式不一样。面试题更适合讲思路、表达、知识点；认证题更适合解释选项、考点、为什么选这个。
        3）后面扩展方便，比如以后你想加一些新的字段。
        4）可读性好，你自己调试 prompt 时，一眼能看出最终喂给 AI 的上下文长什么样。
         */
    }

    private String buildCertificateQuestionContext(AiFollowUpDTO dto) {
        // 变更：认证题 AI 上下文也直接在题目查询中校验 bank_id。
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
                """.formatted(question.getTitle(), question.getCorrectAnswer(), question.getAnalysis());
    }
}
