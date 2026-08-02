package com.homework.web.admin.vo;

import com.homework.model.enums.QuestionImportStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 题目 Excel 导入任务状态。 */
@Data
public class QuestionImportTaskVO {

    /** 对外任务编号。 */
    private String taskId;

    /** 目标题库 ID。 */
    private Long bankId;

    /** 原始文件名。 */
    private String fileName;

    /** 当前任务状态名称。 */
    private QuestionImportStatus status;

    /** Excel 数据总行数。 */
    private Integer totalRows;

    /** 校验通过行数。 */
    private Integer validRows;

    /** 校验失败行数。 */
    private Integer errorRows;

    /** 已导入行数。 */
    private Integer importedRows;

    /** 失败原因。 */
    private String failureReason;

    /** 任务到期时间。 */
    private LocalDateTime expiresTime;

    /** 导入完成时间。 */
    private LocalDateTime finishedTime;
}
