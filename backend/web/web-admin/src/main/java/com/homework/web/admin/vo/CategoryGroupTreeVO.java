package com.homework.web.admin.vo;

import lombok.Data;

import java.util.List;

/** 后台只读分类树中的一级分类。 */
@Data
public class CategoryGroupTreeVO {

    /** 一级分类 ID。 */
    private Long id;

    /** 一级分类名称。 */
    private String groupName;

    /** 面试或认证类型。 */
    private String groupType;

    /** 该一级分类下的二级分类。 */
    private List<CategoryModuleTreeVO> modules;
}
