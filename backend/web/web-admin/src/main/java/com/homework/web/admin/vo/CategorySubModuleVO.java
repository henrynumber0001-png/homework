package com.homework.web.admin.vo;

import lombok.Data;

/** 后台分类树中的三级分类。 */
@Data
public class CategorySubModuleVO {

    /** 三级分类 ID。 */
    private Long id;

    /** 三级分类名称。 */
    private String subModuleName;

    /** 展示顺序。 */
    private Integer sortOrder;
}
