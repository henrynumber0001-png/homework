package com.homework.web.admin.vo;

import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
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
    private GroupType groupType;

    /** 题型名称。 */
    private QuestionInfoQuestionType questionType;

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

    /** 变更：一题只属于一个题库，手动顺序直接来自题目实体。 */
    private Integer sortOrder;

    /** 乐观锁版本。 */
    private Integer version;
}
