package com.homework.web.admin.vo;

import lombok.Data;

/** Excel 题目导入错误行。 */
@Data
public class QuestionImportErrorVO {

    /** Excel 原始行号。 */
    private Integer rowNumber;

    /** 出错字段。 */
    private String fieldName;

    /** 可展示的错误原因。 */
    private String errorMessage;
}
