package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.admin.dto.QuestionOptionDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionContentServiceTest {

    private final QuestionContentService service = new QuestionContentService();

    @Test
    void interviewBankOnlyAcceptsEssay() {
        QuestionInfoQuestionType type = service.parseAndValidate(
                GroupType.INTERVIEW,
                "ESSAY",
                null,
                null
        );

        assertEquals(QuestionInfoQuestionType.ESSAY, type);
        assertThrows(HomeworkException.class, () -> service.parseAndValidate(
                GroupType.INTERVIEW,
                "SINGLE_CHOICE",
                options("A", "B"),
                List.of("A")
        ));
    }

    @Test
    void multipleChoiceRequiresAtLeastTwoCorrectKeys() {
        assertThrows(HomeworkException.class, () -> service.parseAndValidate(
                GroupType.CERTIFICATION,
                "MULTIPLE",
                options("A", "B"),
                List.of("A")
        ));

        QuestionInfoQuestionType type = service.parseAndValidate(
                GroupType.CERTIFICATION,
                "MULTIPLE",
                options("A", "B"),
                List.of("A", "B")
        );
        assertEquals(QuestionInfoQuestionType.MULTIPLE, type);
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
