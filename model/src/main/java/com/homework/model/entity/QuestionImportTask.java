package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionImportStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Excel 题目预检与导入任务。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_import_task")
public class QuestionImportTask extends BaseEntity {

    /** 对外展示的任务编号。 */
    private String taskNo;

    /** 目标题库 ID。 */
    private Long bankId;

    /** 创建任务的管理员 ID。 */
    private Long adminId;

    /** 原始文件名。 */
    private String fileName;

    /** 上传文件 SHA-256。 */
    private String fileSha256;

    /** 临时文件绝对路径。 */
    private String filePath;

    /** 当前任务状态。 */
    private QuestionImportStatus status;

    /** Excel 数据总行数。 */
    private Integer totalRows;

    /** 通过校验的行数。 */
    private Integer validRows;

    /** 校验失败的行数。 */
    private Integer errorRows;

    /** 已成功导入的行数。 */
    private Integer importedRows;

    /** 任务失败原因。 */
    private String failureReason;

    /** 临时任务到期时间。 */
    private LocalDateTime expiresTime;

    /** 导入完成时间。 */
    private LocalDateTime finishedTime;
}
