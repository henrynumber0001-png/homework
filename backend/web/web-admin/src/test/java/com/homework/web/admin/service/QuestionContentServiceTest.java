package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.admin.dto.QuestionOptionDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionContentServiceTest {

    private final QuestionContentService service = new QuestionContentService();

    @Test
    void interviewBankOnlyAcceptsEssay() {
        service.validateQuestionCreation(
                GroupType.INTERVIEW,
                QuestionInfoQuestionType.ESSAY,
                null,
                null
        );

        assertThrows(HomeworkException.class, () -> service.validateQuestionCreation(
                GroupType.INTERVIEW,
                QuestionInfoQuestionType.SINGLE_CHOICE,
                options("A", "B"),
                List.of("A")
        ));
    }

    @Test
    void multipleChoiceRequiresAtLeastTwoCorrectKeys() {
        assertThrows(HomeworkException.class, () -> service.validateQuestionCreation(
                GroupType.CERTIFICATION,
                QuestionInfoQuestionType.MULTIPLE,
                options("A", "B"),
                List.of("A")
        ));

        service.validateQuestionCreation(
                GroupType.CERTIFICATION,
                QuestionInfoQuestionType.MULTIPLE,
                options("A", "B"),
                List.of("A", "B")
        );
    }

    @Test
    void choiceAcceptsTwoToSixActualOptions() {
        service.validateQuestionCreation(
                GroupType.CERTIFICATION,
                QuestionInfoQuestionType.SINGLE_CHOICE,
                options("选项 A", "选项 B", "选项 C", "选项 D"),
                List.of("A")
        );

        assertThrows(HomeworkException.class, () -> service.validateQuestionCreation(
                GroupType.CERTIFICATION,
                QuestionInfoQuestionType.SINGLE_CHOICE,
                options("选项 A", "选项 B", "选项 C", "选项 D", "选项 E", "选项 F", "选项 G"),
                List.of("A")
        ));
    }

    @Test
    void correctKeyMustReferenceAnExistingOption() {
        assertThrows(HomeworkException.class, () -> service.validateQuestionCreation(
                GroupType.CERTIFICATION,
                QuestionInfoQuestionType.SINGLE_CHOICE,
                options("选项 A", "选项 B"),
                List.of("C")
        ));
    }

    @Test
    void correctAnswerElementMustNotBeNull() {
        List<String> correctAnswerKeys = new ArrayList<>();
        correctAnswerKeys.add(null);

        assertThrows(HomeworkException.class, () -> service.validateQuestionCreation(
                GroupType.CERTIFICATION,
                QuestionInfoQuestionType.SINGLE_CHOICE,
                options("选项 A", "选项 B"),
                correctAnswerKeys
        ));
    }

    @Test
    void correctKeysAreStoredAsAppOptionContents() {
        List<QuestionOptionDTO> options = options("第一项", "第二项", "第三项");

        assertEquals(
                List.of("第一项", "第三项"),
                service.toCorrectAnswerContents(options, List.of("A", "C"))
        );
    }

    private List<QuestionOptionDTO> options(String... contents) {
        java.util.ArrayList<QuestionOptionDTO> options = new java.util.ArrayList<>();
        for (int index = 0; index < contents.length; index++) {
            QuestionOptionDTO option = new QuestionOptionDTO();
            option.setKey(String.valueOf((char) ('A' + index)));
            option.setContent(contents[index]);
            options.add(option);
        }
        return options;
    }
}
