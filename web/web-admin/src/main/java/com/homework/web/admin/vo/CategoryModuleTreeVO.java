package com.homework.web.admin.vo;

import lombok.Data;

import java.util.List;

/** 后台分类树中的二级分类及其子分类。 */
@Data
public class CategoryModuleTreeVO {

    /** 二级分类 ID。 */
    private Long id;

    /** 二级分类名称。 */
    private String moduleName;

    /** 展示顺序。 */
    private Integer sortOrder;

    /** 该模块下的三级分类。 */
    private List<CategorySubModuleVO> subModules;
}
