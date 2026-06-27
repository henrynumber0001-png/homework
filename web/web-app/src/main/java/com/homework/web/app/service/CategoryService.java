package com.homework.web.app.service;

import com.homework.web.app.dto.CategoryDetailSaveDTO;
import com.homework.web.app.dto.CategorySubModuleSaveDTO;
import com.homework.web.app.vo.CategoryDetailVO;
import com.homework.web.app.vo.CategoryModuleVO;
import com.homework.web.app.vo.CategorySubModuleVO;

import java.util.List;

public interface CategoryService {
    List<CategoryModuleVO> listModules(Long groupId);
    List<CategorySubModuleVO> listSubModules(Long moduleId);
    List<CategoryDetailVO> listDetails(Long subModuleId);
    Long saveSubModule(CategorySubModuleSaveDTO dto);
    Long saveDetail(CategoryDetailSaveDTO dto);
    void deleteSubModule(Long id);
    void deleteDetail(Long id);
}
