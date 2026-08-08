package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
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

/**
 * 创建并解析面试题或认证题 Excel 模板。
 *
 * <p>这个类负责两个方向的转换：</p>
 * <ol>
 *     <li>{@link #createTemplate(GroupType)}：根据题库类型生成 Excel 模板，并转换为可下载的字节数组。</li>
 *     <li>{@link #parse(InputStream, GroupType)}：读取管理员上传的 Excel，把每个有效数据行转换为 {@link QuestionCreateDTO}。</li>
 * </ol>
 *
 * <p>Excel 导入只处理题目文字、选项和正确答案，不处理题目图片。题目导入为草稿后，
 * 管理员可以进入单题编辑页上传图片。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionImportWorkbookService {
    // 认证题从下标 3 开始读取 optionA～optionF，因此对应的 Excel 可见列是第 4～9 列。
    // 面试题没有选项，所以不会使用下面三个常量。
    private static final int OPTION_START_COLUMN_INDEX = 3;
    private static final int MAX_OPTION_COUNT = 6;
    // 正确答案列紧跟在 6 个选项列之后：3 + 6 = 9，即 Excel 中可见的第 10 列。
    private static final int CORRECT_ANSWER_KEYS_COLUMN_INDEX = OPTION_START_COLUMN_INDEX + MAX_OPTION_COUNT;

    // 复用单题创建时的规则，保证 Excel 导入和管理员手工创建题目的校验标准一致。
    private final QuestionContentService contentService;


    // Apache POI（Java 操作 Excel 的库）
    // 在内存中创建一个 Excel 文件，填好表头和一行示例数据，然后返回这个 Excel 文件的二进制数据。
    public byte[] createTemplate(GroupType groupType) {
        // 为什么用try-with-resources ？
        // 因为workbook 和 output 都属于资源，写完必须关闭，否则可能内存泄露
        // 因此写在try()里面，以保证workbook 和 output 在方法结束时自动关闭。
        try (XSSFWorkbook workbook = new XSSFWorkbook(); //新建一个空的Excel；
             ByteArrayOutputStream output = new ByteArrayOutputStream()) //新建一个内存输出流，Excel会写到这里：workbook -> output -> byte[]
        {
           //这是在创建Excel中的一个sheet，命名为questions
            Sheet sheet = workbook.createSheet("questions");
            //创建sheet中的第一行
            Row header = sheet.createRow(0);

            // 两种题库都具有这三个基础字段，并且 Excel 模板不包含 imageObjectKey。
            // 准备列名，Apache POI 的列下标从 0 开始：0 = questionType，1 = title，2 = analysis。
            // 这里准备的是一个 List集合，用于存储列名。
            // 请注意⚠️：此时，colums还不是真的列名呢，这里是列名的一个集合
            List<String> columns = new ArrayList<>(List.of("questionType", "title", "analysis"));
            if (groupType == GroupType.CERTIFICATION) {
                // 认证题额外加入 optionA～optionF
                for (int optionIndex = 0; optionIndex < MAX_OPTION_COUNT; optionIndex++) {
                    columns.add("option" + (char) ('A' + optionIndex));
                }
                // 继续添加 正确答案列
                columns.add("correctAnswerKeys");
            }

            //
            for (int index = 0; index < columns.size(); index++) {
                // 直到这一步，才根据行的单元格索引 和 列的集合索引，填对应的字段名
                header.createCell(index).setCellValue(columns.get(index));
                // 设置这一列有多宽多高。
                sheet.setColumnWidth(index, Math.min(50, Math.max(15, columns.get(index).length() + 4)) * 256);
            }

            // 创建第二行，提供一条可直接通过预检的填写示例。
            Row example = sheet.createRow(1);
            if (groupType == GroupType.INTERVIEW) {
                example.createCell(0).setCellValue("ESSAY");
                example.createCell(1).setCellValue("请输入题干");
                example.createCell(2).setCellValue("请输入参考答案");
            } else {
                example.createCell(0).setCellValue("SINGLE_CHOICE");
                example.createCell(1).setCellValue("请输入题干");
                example.createCell(2).setCellValue("请输入答案解析");
                example.createCell(OPTION_START_COLUMN_INDEX).setCellValue("选项 A");
                example.createCell(OPTION_START_COLUMN_INDEX + 1).setCellValue("选项 B");
                example.createCell(CORRECT_ANSWER_KEYS_COLUMN_INDEX).setCellValue("A");
            }

            // 把内存里的 Workbook 对象，转换成真正的 .xlsx 文件格式。
            // workbook对象 -> Apache POI -> ZIP压缩 -> xlsx格式 -> 写入outputStream
            // Apache POI 已经把 Workbook 按照 .xlsx 文件格式写进去了
            workbook.write(output);
            return output.toByteArray(); //toByteArray() 什么都没有转换，它只是把里面已经存在的数据取出来。 为什么要这么麻烦？因为write操作没有返回值
        } catch (IOException exception) {
            // 模板生成失败属于服务端文件处理异常，对外统一转换为业务异常。
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
    }

    /**
     * 解析管理员上传的 Excel，并对每个非空数据行执行题目创建规则校验。
     *
     * <p>单行出错不会立刻终止整个解析过程，而是记录到 {@code errors}，这样管理员一次预检
     * 就能看到文件中的全部行错误。工作簿损坏、模板不合法或超过 1,000 行等文件级错误，
     * 才会直接抛出异常。</p>
     *
     * @param inputStream 上传的 .xlsx 文件输入流
     * @param groupType   目标题库类型，用于决定是否读取选择题列以及校验允许的题型
     * @return 通过校验的题目、逐行错误和非空数据行总数
     */

    // 接收管理员上传的 Excel，然后逐行读取，把合法的数据转换成 QuestionCreateDTO，同时记录哪些行有错误。
    // inputStream 就是 excel 文件，groupType 是题目类型，用于决定是否读取选择题列以及校验允许的题型
    public QuestionImportWorkbookResult parse(InputStream inputStream, GroupType groupType) {

        List<QuestionCreateDTO> questions = new ArrayList<>(); // 存放校验通过的题目
        List<QuestionImportErrorVO> errors = new ArrayList<>(); // 存的是失败记录
        int totalRows = 0; // 统计真正的数据行数量

        // DataFormatter 按 Excel 中显示的内容读取单元格，避免分别判断字符串、数字等单元格类型。
        DataFormatter formatter = new DataFormatter(Locale.ROOT); // 统一当成字符串处理

        // 从输入流中读取一个现有的 .xlsx 文件，并解析成 Workbook 对象。
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            // 获取第一个sheet 和 第一行
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);

            // 至少检查前两个关键表头，避免把完全无关的 Excel 当成题目文件解析。
            if (header == null || !"questionType".equals(formatter.formatCellValue(header.getCell(0)))
                    || !"title".equals(formatter.formatCellValue(header.getCell(1)))) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
            }

            // rowIndex 从 1 开始，因为下标 0 是表头；getLastRowNum() 返回最后一个实际行下标。
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                // 空行不算数据，也不占用 1,000 行的导入额度。
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                totalRows++; // 第0行，压根就没计算进去，因为是表头行（Row header = sheet.getRow(0)），所以是从 rowIndex = 1 开始计算的

                if (totalRows > 1000) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
                }

                // 先把这一行的基础单元格转换为创建题目使用的 DTO。
                QuestionCreateDTO dto = new QuestionCreateDTO();
                String questionTypeValue = formatter.formatCellValue(row.getCell(0)).trim();
                dto.setTitle(formatter.formatCellValue(row.getCell(1)).trim());
                dto.setAnalysis(formatter.formatCellValue(row.getCell(2)).trim());

                /*
                 * optionGap 表示已经在选项序列中遇到空白，例如填写了 A、B，C 为空。
                 * 如果之后的 D 又有内容，则 optionGapViolation 为 true，因为选项必须连续填写。
                 */
                boolean optionGap = false; //这个变量指的是中间的选项出现空格的情况，如果第一个选项就是空格，要在QuestionContentService中处理
                boolean optionGapViolation = false; //这个变量也是为了应对，中间选项出现空格，之后又有非空格选项的情况
                if (groupType == GroupType.CERTIFICATION) { //只有认证题才能读取选项
                    List<QuestionOptionDTO> options = new ArrayList<>();
                    for (int optionIndex = 0; optionIndex < MAX_OPTION_COUNT; optionIndex++) {
                        String content = formatter.formatCellValue(
                                row.getCell(OPTION_START_COLUMN_INDEX + optionIndex)
                        ).trim();
                        if (content.isEmpty()) {
                            if (!options.isEmpty()) {
                                optionGap = true;
                            }
                            continue;
                        }
                        if (optionGap) { //中间出现空格
                            optionGapViolation = true; //空格后又有选项，属于非法
                            break; //结束循环
                        }
                        QuestionOptionDTO option = new QuestionOptionDTO();
                        // optionIndex 为 0～5，通过字符偏移生成 A～F，避免管理员在 Excel 中另填选项 Key。
                        option.setKey(String.valueOf((char) ('A' + optionIndex)));
                        option.setContent(content);
                        options.add(option); // options 只是一开始的时候是一个空的List集合，如果第一个选项的内容不为空，那么就会被加入到 options 中
                    }
                    dto.setOptions(options);

                    // 正确答案同时兼容英文逗号和中文逗号，例如 A,C 或 A，C。
                    String correctAnswerKeys = formatter.formatCellValue(row.getCell(CORRECT_ANSWER_KEYS_COLUMN_INDEX)).trim();
                    dto.setCorrectAnswerKeys(correctAnswerKeys.isEmpty() ?
                            List.of() :
                            Arrays.stream(correctAnswerKeys.split("[,，]")) //表示同时支持 英文逗号和中文逗号
                            .map(String::trim).filter(value -> !value.isEmpty()).toList());
                    //虽然List<@NotBlank String> correctAnswerKeys 中有 @NotBlank，但是Excel 导入过程中，@NotBlank 不会自动运行，因此还是需要isEmpty()主动过滤一下
                }

                // 以下异常都只代表当前行不合法，因此在内部捕获并转换为一条预检错误。
                try {
                    if (optionGapViolation) {
                        throw new IllegalArgumentException("选项必须从 A 开始连续填写");
                    }

                    // 基础长度约束：题型和题干必填；题干最多 5,000 字，解析最多 20,000 字。
                    if (questionTypeValue.isBlank() || dto.getTitle().isBlank()
                            || dto.getTitle().length() > 5000
                            || dto.getAnalysis() != null && dto.getAnalysis().length() > 20000) {
                        throw new IllegalArgumentException("题型、题干或解析不符合长度要求");
                    }

                    // 将 Excel 中的字符串（例如 single_choice）标准化为大写后转换成题型枚举。
                    dto.setQuestionType(QuestionInfoQuestionType.valueOf(
                            questionTypeValue.toUpperCase(Locale.ROOT)
                    ));

                    // 继续校验题库与题型是否匹配，以及选项数量、顺序、重复项和正确答案是否合法。
                    // A选项就是空的情况，在这一步会检查到校验失败
                    contentService.validateQuestionCreation(
                            groupType,
                            dto.getQuestionType(),
                            dto.getOptions(),
                            dto.getCorrectAnswerKeys()
                    );
                    questions.add(dto);
                } catch (RuntimeException exception) {
                    // POI 的 rowIndex 从 0 开始，Excel 展示的行号从 1 开始，所以这里需要 +1。
                    QuestionImportErrorVO error = new QuestionImportErrorVO();
                    error.setRowNumber(rowIndex + 1);
                    error.setFieldName("row");
                    error.setErrorMessage(exception.getMessage() == null ? "题目数据不合法" : exception.getMessage());
                    errors.add(error);
                }
            }
        } catch (HomeworkException exception) {
            // 已经明确分类的业务异常直接向上抛出，避免被重新包装后丢失错误码。
            throw exception;
        } catch (Exception exception) {
            // 文件损坏、并非合法的 .xlsx 或其他 POI 解析异常，统一视为导入文件不合法。
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID, exception);
        }

        // 只有表头而没有任何非空数据行的文件不能创建导入任务。
        if (totalRows == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
        }

        // 返回预检结果；后续服务根据 errors 是否为空决定任务能否进入 READY 状态。
        QuestionImportWorkbookResult result = new QuestionImportWorkbookResult();
        result.setQuestions(questions);
        result.setErrors(errors);
        result.setTotalRows(totalRows);
        return result;
    }

    /**
     * 判断一行是否完全为空。
     *
     * @param row       POI 读取到的 Excel 行
     * @param formatter 与主解析流程共用的单元格格式化器
     * @return 所有已定义单元格都为空时返回 {@code true}，任一单元格有内容时返回 {@code false}
     */
    public boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
