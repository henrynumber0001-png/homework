package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 题库题目顺序保存结果。 */
@Data
public class QuestionOrderResultVO {

    /** 题库 ID。 */
    private Long bankId;

    /** 已排序题目数量。 */
    private Integer questionCount;

    /** 新的题目顺序版本。 */
    private Integer bankQuestionOrderVersion;

    /** 题库更新时间。 */
    private LocalDateTime updatedTime;
}
