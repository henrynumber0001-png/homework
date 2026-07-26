package com.homework.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.SortType;
import com.homework.web.app.vo.GroupPageVO;
import com.homework.web.app.vo.ModulePageVO;
import com.homework.web.app.vo.QuestionBankVO;
import com.homework.web.app.vo.SubModulePageVO;

import java.util.List;

public interface QuestionBankService extends IService<QuestionBank> {

    GroupPageVO getGroupPage(Long groupId);

    ModulePageVO getModulePage(Long currentGroupId, Long moduleId, Long currentModuleId);

    SubModulePageVO getSubModulePage(Long currentGroupId, Long currentModuleId, Long subModuleId, Long currentSubModuleId);

    List<QuestionBankVO> getSortType(SortType sortType,Long currentSubModuleId);

//   List<QuestionBankVO> getQuestionBankPage(Long currentGroupId, Long currentModuleId, Long currentSubModuleId, Long questionBankId);

}
