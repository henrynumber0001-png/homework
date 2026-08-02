package com.homework.web.admin.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
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
    private QuestionInfoQuestionType questionType;

    /** 题干。 */
    private String title;

    /** 题干图片地址。 */
    private String imageUrl;

    /** 是否已发布。 */
    private Boolean released;

    /** 变更：关系表已删除，题库内手动顺序直接来自题目实体。 */
    private Integer sortOrder;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 最近更新时间。 */
    private LocalDateTime updatedTime;

    /** 乐观锁版本。 */
    private Integer version;
}
