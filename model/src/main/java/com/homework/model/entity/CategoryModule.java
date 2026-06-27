package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category_module")
public class CategoryModule extends BaseEntity {

    private Long groupId;

    private String moduleName;

    private String displayIcon;

    private String themeColor;

    private String themeBgColor;

    private String themeGradient;

    private Integer sortOrder;
}
