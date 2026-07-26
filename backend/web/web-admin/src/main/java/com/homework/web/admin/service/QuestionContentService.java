package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.admin.dto.QuestionOptionDTO;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 统一校验并转换手工创建、编辑和 Excel 导入的题目内容。 */
@Service
public class QuestionContentService {

    public QuestionInfoQuestionType parseAndValidate(
            GroupType groupType,
            String questionTypeValue,
            List<QuestionOptionDTO> options,
            List<String> correctAnswers
    ) {
        QuestionInfoQuestionType questionType;
        try {
            questionType = QuestionInfoQuestionType.valueOf(questionTypeValue.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID, exception);
        }
        if (groupType == GroupType.INTERVIEW && questionType != QuestionInfoQuestionType.ESSAY) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID);
        }
        if (groupType == GroupType.CERTIFICATION && questionType == QuestionInfoQuestionType.ESSAY) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID);
        }
        if (questionType == QuestionInfoQuestionType.ESSAY) {
            if (options != null && !options.isEmpty() || correctAnswers != null && !correctAnswers.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }
            return questionType;
        }
        if (options == null || options.size() < 2 || options.size() > 26
                || correctAnswers == null || correctAnswers.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
        }
        Set<String> optionContents = new HashSet<>();
        Set<String> optionKeys = new HashSet<>();
        for (int index = 0; index < options.size(); index++) {
            QuestionOptionDTO option = options.get(index);
            String expectedKey = String.valueOf((char) ('A' + index));
            if (option == null || option.getKey() == null
                    || !expectedKey.equals(option.getKey().trim().toUpperCase(Locale.ROOT))
                    || option.getContent() == null || option.getContent().isBlank()
                    || !optionKeys.add(expectedKey) || !optionContents.add(option.getContent().trim())) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }
        }
        List<String> normalizedCorrectAnswers = correctAnswers.stream()
                .map(value -> value == null ? "" : value.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (new HashSet<>(normalizedCorrectAnswers).size() != normalizedCorrectAnswers.size()
                || !optionKeys.containsAll(normalizedCorrectAnswers)
                || questionType == QuestionInfoQuestionType.SINGLE_CHOICE && normalizedCorrectAnswers.size() != 1
                || questionType == QuestionInfoQuestionType.MULTIPLE && normalizedCorrectAnswers.size() < 2) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
        }
        return questionType;
    }

    public List<String> toOptionContents(List<QuestionOptionDTO> options) {
        return options.stream().map(option -> option.getContent().trim()).toList();
    }

    public List<String> toCorrectAnswerContents(
            List<QuestionOptionDTO> options,
            List<String> correctAnswerKeys
    ) {
        return correctAnswerKeys.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(key -> options.get(key.charAt(0) - 'A').getContent().trim())
                .toList();
    }
}
