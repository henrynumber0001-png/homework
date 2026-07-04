package com.homework.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.model.entity.QuestionBank;
import com.homework.web.app.vo.GroupPageVO;
import com.homework.web.app.vo.ModulePageVO;
import com.homework.web.app.vo.SubModulePageVO;

public interface QuestionBankService extends IService<QuestionBank> {

    GroupPageVO getGroupPage(Long groupId);

    ModulePageVO getModulePage(Long groupId, Long moduleId);

    SubModulePageVO getSubModulePage(Long groupId, Long moduleId, Long subModuleId);
}
