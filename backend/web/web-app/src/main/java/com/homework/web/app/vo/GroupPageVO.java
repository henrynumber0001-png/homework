package com.homework.web.app.vo;

import com.homework.model.enums.SortType;
import lombok.Data;

import java.util.List;

@Data
public class GroupPageVO {

    //高亮的是第一个module
    private CategoryModuleVO firstModule;
    //高亮的是第一个sub_module
    private CategorySubModuleVO firstSubModule;

    private SortType sort;

    private List<CategoryModuleVO> modules;

    private List<CategorySubModuleVO> subModules;

    private List<QuestionBankVO> banks;
}
