package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台题目列表行。 */
@Data
public class QuestionRowVO {

    /** 题目 ID。 */
    private Long id;

    /** 当前查询题库 ID。 */
    private Long bankId;

    /** 题型名称。 */
    private String questionType;

    /** 题干。 */
    private String title;

    /** 题干图片地址。 */
    private String imageUrl;

    /** 是否已发布。 */
    private Boolean released;

    /** 是否已逻辑删除。 */
    private Boolean deleted;

    /** 当前题库中的排序值。 */
    private Integer bankSortOrder;

    /** 引用该题目的题库数量。 */
    private Long referencedBankCount;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 最近更新时间。 */
    private LocalDateTime updatedTime;

    /** 乐观锁版本。 */
    private Integer version;
}
