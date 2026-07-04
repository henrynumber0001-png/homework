package com.homework.web.app.service;

import com.homework.web.app.dto.CategorySubModuleSaveDTO;
import com.homework.web.app.vo.CategoryModuleVO;

import java.util.List;

public interface CategoryService {
    List<CategoryModuleVO> listModules(Long groupId);
    List<CategorySubModuleVO> listSubModules(Long moduleId);
    Long saveSubModule(CategorySubModuleSaveDTO dto);
    void deleteSubModule(Long id);
}
