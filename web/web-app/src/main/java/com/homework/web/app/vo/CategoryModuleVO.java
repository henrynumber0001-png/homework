package com.homework.web.app.vo;

import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.GraphInfo;
import lombok.Data;

import java.util.List;

@Data
public class CategoryModuleVO  {

    private Long id;

    private Long groupId;

    private String moduleName;

    private Integer sortOrder;
    /** 模块背景图片 URL。 */
    private GraphInfoVo graphInfoVo;

}
