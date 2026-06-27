package com.homework.web.app.service;

import com.homework.web.app.dto.PracticeSessionCreateDTO;
import com.homework.web.app.dto.QuestionAnswerSubmitDTO;

import java.util.Map;

public interface PracticeService {
    Map<String, Object> createSession(PracticeSessionCreateDTO dto);
    Map<String, Object> submitAnswer(QuestionAnswerSubmitDTO dto);
    Map<String, Object> submitSession(Long sessionId);
    Map<String, Object> getSessionResult(Long sessionId);
}
