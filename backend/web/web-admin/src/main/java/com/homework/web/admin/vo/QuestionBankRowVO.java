package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 后台题库列表记录。 */
@Data
public class QuestionBankRowVO {

    /** 题库 ID。 */
    private Long id;

    /** 题库名称。 */
    private String bankName;

    /** 题库所属一级类型。 */
    private String groupType;

    /** 一级分类。 */
    private NamedIdVO group;

    /** 二级分类。 */
    private NamedIdVO module;

    /** 三级分类。 */
    private NamedIdVO subModule;

    /** 题库业务状态。 */
    private String status;

    /** 题库标签。 */
    private List<String> tags;

    /** 运营优先级。 */
    private Integer priority;

    /** 题目总数。 */
    private Long questionCount;

    /** 已发布题目数。 */
    private Long releasedQuestionCount;

    /** 累计浏览次数。 */
    private Integer viewCount;

    /** 累计完成人数。 */
    private Integer completeCount;

    /** 首次发布时间。 */
    private LocalDateTime publishedTime;

    /** 最近更新时间。 */
    private LocalDateTime updatedTime;

    /** 乐观锁版本。 */
    private Integer version;
}
