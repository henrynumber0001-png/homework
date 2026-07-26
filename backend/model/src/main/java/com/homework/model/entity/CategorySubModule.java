package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category_sub_module")
public class CategorySubModule extends BaseEntity {

    private Long moduleId;

    @TableField( "sub_module_name")
    private String subModuleName;

    private Integer sortOrder;
}
