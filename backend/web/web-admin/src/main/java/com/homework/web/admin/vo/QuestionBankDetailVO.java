package com.homework.web.admin.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 后台题库详情。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionBankDetailVO extends QuestionBankRowVO {

    /** 创建题库的管理员。 */
    private AdminSummaryVO createAdmin;

    /** 题库创建时间。 */
    private LocalDateTime createdTime;

    /** 是否已逻辑删除。 */
    private Boolean deleted;

    /** 最近一次删除原因。 */
    private String deleteReason;
}
