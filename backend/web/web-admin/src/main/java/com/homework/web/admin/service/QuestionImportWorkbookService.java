package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.enums.GroupType;
import com.homework.web.admin.dto.QuestionCreateDTO;
import com.homework.web.admin.dto.QuestionOptionDTO;
import com.homework.web.admin.vo.QuestionImportErrorVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 创建并解析面试题或认证题 Excel 模板。 */
@Service
@RequiredArgsConstructor
public class QuestionImportWorkbookService {

    private final QuestionContentService contentService;
    private final QuestionImageService imageService;

    public byte[] createTemplate(GroupType groupType) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("questions");
            Row header = sheet.createRow(0);
            List<String> columns = new ArrayList<>(List.of("questionType", "title", "analysis", "imageUploadId"));
            if (groupType == GroupType.CERTIFICATION) {
                for (char key = 'A'; key <= 'Z'; key++) {
                    columns.add("option" + key);
                }
                columns.add("correctAnswers");
            }
            for (int index = 0; index < columns.size(); index++) {
                header.createCell(index).setCellValue(columns.get(index));
                sheet.setColumnWidth(index, Math.min(50, Math.max(15, columns.get(index).length() + 4)) * 256);
            }
            Row example = sheet.createRow(1);
            if (groupType == GroupType.INTERVIEW) {
                example.createCell(0).setCellValue("ESSAY");
                example.createCell(1).setCellValue("请输入题干");
                example.createCell(2).setCellValue("请输入参考答案");
            } else {
                example.createCell(0).setCellValue("SINGLE_CHOICE");
                example.createCell(1).setCellValue("请输入题干");
                example.createCell(2).setCellValue("请输入答案解析");
                example.createCell(4).setCellValue("选项 A");
                example.createCell(5).setCellValue("选项 B");
                example.createCell(30).setCellValue("A");
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
    }

    public QuestionImportWorkbookResult parse(InputStream inputStream, GroupType groupType) {
        List<QuestionCreateDTO> questions = new ArrayList<>();
        List<QuestionImportErrorVO> errors = new ArrayList<>();
        int totalRows = 0;
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || !"questionType".equals(formatter.formatCellValue(header.getCell(0)))
                    || !"title".equals(formatter.formatCellValue(header.getCell(1)))) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
            }
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }
                totalRows++;
                if (totalRows > 1000) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
                }
                QuestionCreateDTO dto = new QuestionCreateDTO();
                dto.setQuestionType(formatter.formatCellValue(row.getCell(0)).trim());
                dto.setTitle(formatter.formatCellValue(row.getCell(1)).trim());
                dto.setAnalysis(formatter.formatCellValue(row.getCell(2)).trim());
                String imageUploadId = formatter.formatCellValue(row.getCell(3)).trim();
                dto.setImageUploadId(imageUploadId.isEmpty() ? null : imageUploadId);
                boolean optionGap = false;
                boolean optionGapViolation = false;
                if (groupType == GroupType.CERTIFICATION) {
                    List<QuestionOptionDTO> options = new ArrayList<>();
                    for (int optionIndex = 0; optionIndex < 26; optionIndex++) {
                        String content = formatter.formatCellValue(row.getCell(4 + optionIndex)).trim();
                        if (content.isEmpty()) {
                            if (!options.isEmpty()) {
                                optionGap = true;
                            }
                            continue;
                        }
                        if (optionGap) {
                            optionGapViolation = true;
                            break;
                        }
                        QuestionOptionDTO option = new QuestionOptionDTO();
                        option.setKey(String.valueOf((char) ('A' + optionIndex)));
                        option.setContent(content);
                        options.add(option);
                    }
                    dto.setOptions(options);
                    String correctAnswers = formatter.formatCellValue(row.getCell(30)).trim();
                    dto.setCorrectAnswers(correctAnswers.isEmpty()
                            ? List.of()
                            : Arrays.stream(correctAnswers.split("[,，]"))
                                    .map(String::trim)
                                    .filter(value -> !value.isEmpty())
                                    .toList());
                }
                try {
                    if (optionGapViolation) {
                        throw new IllegalArgumentException("选项必须从 A 开始连续填写");
                    }
                    if (dto.getQuestionType().isBlank() || dto.getTitle().isBlank()
                            || dto.getTitle().length() > 5000
                            || dto.getAnalysis() != null && dto.getAnalysis().length() > 20000) {
                        throw new IllegalArgumentException("题型、题干或解析不符合长度要求");
                    }
                    contentService.parseAndValidate(
                            groupType,
                            dto.getQuestionType(),
                            dto.getOptions(),
                            dto.getCorrectAnswers()
                    );
                    if (dto.getImageUploadId() != null) {
                        imageService.validateUploadId(dto.getImageUploadId());
                    }
                    questions.add(dto);
                } catch (RuntimeException exception) {
                    QuestionImportErrorVO error = new QuestionImportErrorVO();
                    error.setRowNumber(rowIndex + 1);
                    error.setFieldName("row");
                    error.setErrorMessage(exception.getMessage() == null ? "题目数据不合法" : exception.getMessage());
                    errors.add(error);
                }
            }
        } catch (HomeworkException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID, exception);
        }
        if (totalRows == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
        }
        QuestionImportWorkbookResult result = new QuestionImportWorkbookResult();
        result.setQuestions(questions);
        result.setErrors(errors);
        result.setTotalRows(totalRows);
        return result;
    }

    public boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
