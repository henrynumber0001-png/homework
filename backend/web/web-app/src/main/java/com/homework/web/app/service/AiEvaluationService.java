package com.homework.web.app.service;

import com.homework.web.app.dto.AiEvaluationResult;

public interface AiEvaluationService {
    AiEvaluationResult evaluateInterviewAnswer(String title,String content,String analysis);
}
