package com.homework.web.app.service;

import com.homework.web.app.dto.QuestionSaveDTO;
import com.homework.web.app.vo.QuestionVO;

import java.util.List;

public interface QuestionService {
    List<QuestionVO> listQuestionsByBank(Long bankId);
    QuestionVO getQuestion(Long questionId);
    Long saveQuestion(QuestionSaveDTO dto);
    void deleteQuestion(Long id);
    java.util.Map<String, Object> favoriteQuestion(Long userId, Long questionId);
}
