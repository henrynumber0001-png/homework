package com.homework.web.admin.service;

import com.homework.web.admin.dto.QuestionCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 在单一事务中把预检通过的全部题目写入目标题库。 */
@Service
@RequiredArgsConstructor
public class QuestionImportCommitService {

    private final AdminQuestionService questionService;

    @Transactional
    public int importAll(Long bankId, List<QuestionCreateDTO> questions) {
        for (QuestionCreateDTO question : questions) {
            questionService.create(bankId, question);
        }
        return questions.size();
    }
}
