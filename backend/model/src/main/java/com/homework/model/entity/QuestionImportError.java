package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Excel 题目导入的逐行错误明细。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_import_error")
public class QuestionImportError extends BaseEntity {

    /** 所属导入任务 ID。 */
    private Long taskId;

    /** Excel 原始行号。 */
    @TableField("source_row_number")
    private Integer rowNumber;

    /** 出错字段名。 */
    private String fieldName;

    /** 可直接展示给管理员的错误原因。 */
    private String errorMessage;
}
