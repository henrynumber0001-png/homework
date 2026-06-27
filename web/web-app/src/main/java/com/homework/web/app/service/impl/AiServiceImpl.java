package com.homework.web.app.service.impl;

import com.homework.common.enums.AiChatMessageSenderType;
import com.homework.common.enums.AiChatSessionStatus;
import com.homework.common.enums.AnswerCorrectionFeedbackStatus;
import com.homework.web.app.dto.AiChatMessageDTO;
import com.homework.web.app.dto.CorrectionFeedbackDTO;
import com.homework.model.entity.AiChatMessage;
import com.homework.model.entity.AiChatSession;
import com.homework.model.entity.AnswerCorrectionFeedback;
import com.homework.web.app.mapper.AiChatMessageMapper;
import com.homework.web.app.mapper.AiChatSessionMapper;
import com.homework.web.app.mapper.AnswerCorrectionFeedbackMapper;
import com.homework.web.app.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final AnswerCorrectionFeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public Map<String, Object> ask(AiChatMessageDTO dto) {
        Long sessionId = dto.getSessionId();
        if (sessionId == null) {
            AiChatSession session = new AiChatSession();
            session.setUserId(dto.getUserId());
            session.setQuestionId(dto.getQuestionId());
            session.setAnswerId(dto.getAnswerId());
            session.setTitle("Question follow-up");
            session.setStatus(AiChatSessionStatus.ACTIVE);
            aiChatSessionMapper.insert(session);
            sessionId = session.getId();
        }

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setSenderType(AiChatMessageSenderType.USER);
        userMessage.setMessageContent(dto.getMessageContent());
        aiChatMessageMapper.insert(userMessage);

        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setSenderType(AiChatMessageSenderType.AI);
        aiMessage.setMessageContent("这是模板阶段的 AI 回复。后续可在 AiServiceImpl 中接入真实 LLM。建议从定义、适用场景和易错点三个角度继续学习。");
        aiMessage.setModelName("placeholder-chat");
        aiChatMessageMapper.insert(aiMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("userMessageId", userMessage.getId());
        result.put("aiMessageId", aiMessage.getId());
        result.put("reply", aiMessage.getMessageContent());
        return result;
    }

    @Override
    public Long submitCorrection(CorrectionFeedbackDTO dto) {
        AnswerCorrectionFeedback feedback = new AnswerCorrectionFeedback();
        feedback.setUserId(dto.getUserId());
        feedback.setQuestionId(dto.getQuestionId());
        feedback.setAnswerId(dto.getAnswerId());
        feedback.setFeedbackContent(dto.getFeedbackContent());
        feedback.setStatus(AnswerCorrectionFeedbackStatus.PENDING);
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }
}
