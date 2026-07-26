package com.homework.web.admin.service;

import com.homework.model.enums.GroupType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionImportWorkbookServiceTest {

    @Test
    void certificationTemplateExamplePassesPrevalidation() {
        QuestionImportWorkbookService service = new QuestionImportWorkbookService(
                new QuestionContentService(),
                Mockito.mock(QuestionImageService.class)
        );

        byte[] template = service.createTemplate(GroupType.CERTIFICATION);
        QuestionImportWorkbookResult result = service.parse(
                new ByteArrayInputStream(template),
                GroupType.CERTIFICATION
        );

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getQuestions().size());
        assertTrue(result.getErrors().isEmpty());
    }
}
