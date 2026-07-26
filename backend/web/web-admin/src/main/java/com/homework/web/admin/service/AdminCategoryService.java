package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.CategoryGroup;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.enums.GroupType;
import com.homework.web.admin.mapper.CategoryGroupMapper;
import com.homework.web.admin.mapper.CategoryModuleMapper;
import com.homework.web.admin.mapper.CategorySubModuleMapper;
import com.homework.web.admin.vo.CategoryGroupTreeVO;
import com.homework.web.admin.vo.CategoryModuleTreeVO;
import com.homework.web.admin.vo.CategorySubModuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 组装管理端只读三级分类树。 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryGroupMapper groupMapper;
    private final CategoryModuleMapper moduleMapper;
    private final CategorySubModuleMapper subModuleMapper;

    public List<CategoryGroupTreeVO> getTree(GroupType groupType) {
        LambdaQueryWrapper<CategoryGroup> groupQuery = new LambdaQueryWrapper<>();
        groupQuery.eq(groupType != null, CategoryGroup::getGroupType, groupType)
                .orderByAsc(CategoryGroup::getId);
        List<CategoryGroup> groups = groupMapper.selectList(groupQuery);
        List<CategoryGroupTreeVO> result = new ArrayList<>();

        for (CategoryGroup group : groups) {
            CategoryGroupTreeVO groupVO = new CategoryGroupTreeVO();
            groupVO.setId(group.getId());
            groupVO.setGroupName(group.getGroupName());
            groupVO.setGroupType(group.getGroupType().name());

            List<CategoryModule> modules = moduleMapper.selectList(
                    new LambdaQueryWrapper<CategoryModule>()
                            .eq(CategoryModule::getGroupId, group.getId())
                            .orderByAsc(CategoryModule::getSortOrder)
                            .orderByAsc(CategoryModule::getId)
            );
            List<CategoryModuleTreeVO> moduleVOs = new ArrayList<>();
            for (CategoryModule module : modules) {
                CategoryModuleTreeVO moduleVO = new CategoryModuleTreeVO();
                moduleVO.setId(module.getId());
                moduleVO.setModuleName(module.getModuleName());
                moduleVO.setSortOrder(module.getSortOrder());

                List<CategorySubModule> subModules = subModuleMapper.selectList(
                        new LambdaQueryWrapper<CategorySubModule>()
                                .eq(CategorySubModule::getModuleId, module.getId())
                                .orderByAsc(CategorySubModule::getSortOrder)
                                .orderByAsc(CategorySubModule::getId)
                );
                List<CategorySubModuleVO> subModuleVOs = new ArrayList<>();
                for (CategorySubModule subModule : subModules) {
                    CategorySubModuleVO subModuleVO = new CategorySubModuleVO();
                    subModuleVO.setId(subModule.getId());
                    subModuleVO.setSubModuleName(subModule.getSubModuleName());
                    subModuleVO.setSortOrder(subModule.getSortOrder());
                    subModuleVOs.add(subModuleVO);
                }
                moduleVO.setSubModules(subModuleVOs);
                moduleVOs.add(moduleVO);
            }
            groupVO.setModules(moduleVOs);
            result.add(groupVO);
        }
        return result;
    }
}
