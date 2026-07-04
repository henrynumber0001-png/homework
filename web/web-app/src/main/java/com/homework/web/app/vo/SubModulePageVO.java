package com.homework.web.app.vo;


import com.homework.model.enums.SortType;
import lombok.Data;

import java.util.List;

@Data
public class SubModulePageVO {

    //展示型字段
    private SortType sort;

    private List<QuestionBankVO> banks;
}
