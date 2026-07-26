package com.homework.web.admin.vo;

import lombok.Data;

import java.util.List;

/** 后台题目详情。 */
@Data
public class QuestionDetailVO {

    /** 题目 ID。 */
    private Long id;

    /** 当前查询题库 ID。 */
    private Long bankId;

    /** 题库所属一级类型。 */
    private String groupType;

    /** 题型名称。 */
    private String questionType;

    /** 题干。 */
    private String title;

    /** 题干图片地址。 */
    private String imageUrl;

    /** 参考答案或答案解析。 */
    private String analysis;

    /** 认证题选项；面试题为空数组。 */
    private List<QuestionOptionVO> options;

    /** 正确选项键；面试题为空数组。 */
    private List<String> correctAnswers;

    /** 是否已发布。 */
    private Boolean released;

    /** 是否已逻辑删除。 */
    private Boolean deleted;

    /** 当前题库中的排序值。 */
    private Integer bankSortOrder;

    /** 引用该题目的题库数量。 */
    private Long referencedBankCount;

    /** 当前管理员可见的关联题库。 */
    private List<ReferencedBankVO> visibleReferencedBanks;

    /** 是否还存在当前管理员不可见的关联题库。 */
    private Boolean hasHiddenReferences;

    /** 乐观锁版本。 */
    private Integer version;
}
