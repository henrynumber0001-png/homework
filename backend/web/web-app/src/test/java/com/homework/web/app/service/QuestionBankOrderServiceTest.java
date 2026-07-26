package com.homework.web.app.service;

import com.homework.model.entity.InterviewQuestionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionBankOrderServiceTest {

    @Test
    void relationOrderWinsOverQuestionEntityOrder() {
        InterviewQuestionInfo firstFromDatabase = new InterviewQuestionInfo();
        firstFromDatabase.setId(1L);
        firstFromDatabase.setSortOrder(1);
        InterviewQuestionInfo secondFromDatabase = new InterviewQuestionInfo();
        secondFromDatabase.setId(2L);
        secondFromDatabase.setSortOrder(999);

        List<InterviewQuestionInfo> ordered = new QuestionBankOrderService().orderInterview(
                List.of(firstFromDatabase, secondFromDatabase),
                List.of(2L, 1L)
        );

        assertEquals(List.of(2L, 1L), ordered.stream().map(InterviewQuestionInfo::getId).toList());
    }
}
