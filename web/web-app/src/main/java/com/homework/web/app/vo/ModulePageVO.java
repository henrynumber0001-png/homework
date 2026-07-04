package com.homework.web.app.vo;

import com.homework.model.enums.SortType;
import lombok.Data;

import java.util.List;

@Data
public class ModulePageVO {


    //高亮的是第一个sub_module
    private CategorySubModuleVO firstSubModule;

    //展示型字段
    private SortType sort;

    private List<CategorySubModuleVO> subModules;

    private List<QuestionBankVO> banks;

}
