package com.homework.web.app.service;

import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 按 `question_bank_question.sort_order` 统一排列普通练习题目。 */
@Service
public class QuestionBankOrderService {

    public List<InterviewQuestionInfo> orderInterview(
            List<InterviewQuestionInfo> questions,
            List<Long> orderedQuestionIds
    ) {
        Map<Long, Integer> orderIndex = new HashMap<>();
        for (int index = 0; index < orderedQuestionIds.size(); index++) {
            orderIndex.put(orderedQuestionIds.get(index), index);
        }
        return questions.stream()
                .sorted(Comparator.comparingInt(question ->
                        orderIndex.getOrDefault(question.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    public List<CertificateQuestionInfo> orderCertificate(
            List<CertificateQuestionInfo> questions,
            List<Long> orderedQuestionIds
    ) {
        Map<Long, Integer> orderIndex = new HashMap<>();
        for (int index = 0; index < orderedQuestionIds.size(); index++) {
            orderIndex.put(orderedQuestionIds.get(index), index);
        }
        return questions.stream()
                .sorted(Comparator.comparingInt(question ->
                        orderIndex.getOrDefault(question.getId(), Integer.MAX_VALUE)))
                .toList();
    }
}
