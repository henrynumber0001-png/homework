package com.homework.web.admin.service;

import com.homework.model.enums.GroupType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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

    @Test
    void certificationTemplateContainsSixOptionColumns() throws IOException {
        QuestionImportWorkbookService service = new QuestionImportWorkbookService(
                new QuestionContentService(),
                Mockito.mock(QuestionImageService.class)
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(service.createTemplate(GroupType.CERTIFICATION))
        )) {
            var header = workbook.getSheetAt(0).getRow(0);
            assertEquals("optionA", header.getCell(4).getStringCellValue());
            assertEquals("optionF", header.getCell(9).getStringCellValue());
            assertEquals("correctAnswerKeys", header.getCell(10).getStringCellValue());
            assertEquals(11, header.getLastCellNum());
        }
    }
}
