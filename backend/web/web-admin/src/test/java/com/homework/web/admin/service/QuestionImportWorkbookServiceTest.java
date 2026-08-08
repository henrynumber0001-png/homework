package com.homework.web.admin.service;

import com.homework.model.enums.GroupType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionImportWorkbookServiceTest {

    @Test
    void certificationTemplateExamplePassesPrevalidation() {
        QuestionImportWorkbookService service = new QuestionImportWorkbookService(
                new QuestionContentService()
        );

        byte[] template = service.createTemplate(GroupType.CERTIFICATION);
        QuestionImportWorkbookResult result = service.parse(
                new ByteArrayInputStream(template),
                GroupType.CERTIFICATION
        );

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getQuestions().size());
        assertNull(result.getQuestions().get(0).getImageObjectKey());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void certificationTemplateContainsSixOptionColumns() throws IOException {
        QuestionImportWorkbookService service = new QuestionImportWorkbookService(
                new QuestionContentService()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(service.createTemplate(GroupType.CERTIFICATION))
        )) {
            var header = workbook.getSheetAt(0).getRow(0);
            assertEquals("optionA", header.getCell(3).getStringCellValue());
            assertEquals("optionF", header.getCell(8).getStringCellValue());
            assertEquals("correctAnswerKeys", header.getCell(9).getStringCellValue());
            assertEquals(10, header.getLastCellNum());
        }
    }

    @Test
    void interviewTemplateDoesNotContainImageObjectKeyColumn() throws IOException {
        QuestionImportWorkbookService service = new QuestionImportWorkbookService(
                new QuestionContentService()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(service.createTemplate(GroupType.INTERVIEW))
        )) {
            var header = workbook.getSheetAt(0).getRow(0);
            assertEquals("questionType", header.getCell(0).getStringCellValue());
            assertEquals("title", header.getCell(1).getStringCellValue());
            assertEquals("analysis", header.getCell(2).getStringCellValue());
            assertEquals(3, header.getLastCellNum());
        }
    }
}
