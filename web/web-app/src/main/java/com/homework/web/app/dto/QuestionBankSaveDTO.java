package com.homework.web.app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuestionBankSaveDTO {
    /** 有 id 表示更新已有题库；没有 id 表示新增题库。 */
    private Long id;

    /** 题库展示名称，比如 Java 基础面试题库。 */
    private String bankName;

    /** 所属 sub_module id。 */
    private Long subModuleId;

    /** 完成题库的人数。 */
    private Integer completeUserCount;

    /** 平均正确率。 */
    private BigDecimal avgCorrectRate;

    /** 收藏数量。 */
    private Integer favoriteCount;

    /** 浏览数量。 */
    private Integer viewCount;

    /** 人工排序权重。 */
    private Integer priority;

    /** 题库内题目数量。 */
    private Integer questionCount;

    /** 创建题库的管理员或用户 id。 */
    private Long createUserId;

    /** 发布时间。 */
    private LocalDateTime publishedTime;
}
