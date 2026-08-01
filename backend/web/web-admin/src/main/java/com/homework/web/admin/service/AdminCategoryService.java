package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.CategoryGroup;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
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


@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryGroupMapper groupMapper;
    private final CategoryModuleMapper moduleMapper;
    private final CategorySubModuleMapper subModuleMapper;

    public List<CategoryGroupTreeVO> getTree() {
        // 分类树接口固定返回全部 Group，具体显示和三级联动由前端控制。
        List<CategoryGroup> groups = groupMapper.selectList(
                new LambdaQueryWrapper<CategoryGroup>().orderByAsc(CategoryGroup::getId));

        List<CategoryGroupTreeVO> result = new ArrayList<>();
        for (CategoryGroup group : groups) {
            CategoryGroupTreeVO groupVO = new CategoryGroupTreeVO();
            groupVO.setId(group.getId());
            groupVO.setGroupName(group.getGroupName());
            groupVO.setGroupType(group.getGroupType().name());

            LambdaQueryWrapper<CategoryModule> moduleQuery = new LambdaQueryWrapper<>();
            moduleQuery.eq(CategoryModule::getGroupId, group.getId());
            List<CategoryModule> categoryModuleList = moduleMapper.selectList(moduleQuery);


            List<CategoryModuleTreeVO> modules = new ArrayList<>();
            categoryModuleList.forEach(categoryModule -> {
                CategoryModuleTreeVO moduleVO = new CategoryModuleTreeVO();
                moduleVO.setId(categoryModule.getId());
                moduleVO.setModuleName(categoryModule.getModuleName());
                moduleVO.setSortOrder(categoryModule.getSortOrder());

                LambdaQueryWrapper<CategorySubModule> subModuleQuery = new LambdaQueryWrapper<>();
                subModuleQuery.eq(CategorySubModule::getModuleId, categoryModule.getId());
                List<CategorySubModule> categorySubModules = subModuleMapper.selectList(subModuleQuery);
                List<CategorySubModuleVO> subModules = new ArrayList<>();
                categorySubModules.forEach(categorySubModule -> {
                    CategorySubModuleVO subModuleVO = new CategorySubModuleVO();
                    subModuleVO.setId(categorySubModule.getId());
                    subModuleVO.setSubModuleName(categorySubModule.getSubModuleName());
                    subModuleVO.setSortOrder(categorySubModule.getSortOrder());
                    subModules.add(subModuleVO);
                });
                moduleVO.setSubModules(subModules);
                modules.add(moduleVO);
            });
            groupVO.setModules(modules);
            result.add(groupVO);
        }
        return result;
    }
}
