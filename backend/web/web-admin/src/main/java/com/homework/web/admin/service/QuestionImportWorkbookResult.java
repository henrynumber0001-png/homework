package com.homework.web.admin.service;

import com.homework.web.admin.dto.QuestionCreateDTO;
import com.homework.web.admin.vo.QuestionImportErrorVO;
import lombok.Data;

import java.util.List;

/** 一次 Excel 解析得到的题目和错误集合。 */
@Data
public class QuestionImportWorkbookResult {

    /** 已成功解析并通过规则校验的题目。 */
    private List<QuestionCreateDTO> questions;

    /** 按 Excel 原始行号记录的校验错误。 */
    private List<QuestionImportErrorVO> errors;

    /** 非空数据行总数。 */
    private Integer totalRows;
}
