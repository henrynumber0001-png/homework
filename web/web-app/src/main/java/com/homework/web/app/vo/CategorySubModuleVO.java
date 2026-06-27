package com.homework.web.app.vo;

import lombok.Data;

@Data
public class CategorySubModuleVO {
    private Long id;
    private Long moduleId;
    private String subModuleName;
    private Integer sortOrder;
}
