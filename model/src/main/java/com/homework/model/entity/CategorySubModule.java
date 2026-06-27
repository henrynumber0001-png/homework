package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category_sub_module")
public class CategorySubModule extends BaseEntity {

    private Long moduleId;

    /** For example Java, Spring Boot, MySQL, Linux, AWS Associate. */
    private String subModuleName;

    private Integer sortOrder;
}
