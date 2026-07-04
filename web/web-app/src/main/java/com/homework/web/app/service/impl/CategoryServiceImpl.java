package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.web.app.dto.CategorySubModuleSaveDTO;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.web.app.mapper.CategoryModuleMapper;
import com.homework.web.app.mapper.CategorySubModuleMapper;
import com.homework.web.app.service.CategoryService;
import com.homework.web.app.vo.CategoryModuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryModuleMapper categoryModuleMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;

    @Override
    public List<CategoryModuleVO> listModules(Long groupId) {
        return categoryModuleMapper.selectList(new QueryWrapper<CategoryModule>()
                        .eq(groupId != null, "group_id", groupId)
                        .eq("is_deleted", 0)
                        .orderByAsc("sort_order", "id"))
                .stream().map(this::toModuleVO).toList();
    }

    @Override
    public List<CategorySubModuleVO> listSubModules(Long moduleId) {
        return categorySubModuleMapper.selectList(new QueryWrapper<CategorySubModule>()
                        .eq("module_id", moduleId)
                        .eq("is_deleted", 0)
                        .orderByAsc("sort_order", "id"))
                .stream()
                .map(this::toSubModuleVO)
                .collect(LinkedHashMap<String, CategorySubModuleVO>::new,
                        (map, vo) -> map.putIfAbsent(vo.getSubModuleName(), vo),
                        LinkedHashMap::putAll)
                .values().stream().toList();
    }

    @Override
    public Long saveSubModule(CategorySubModuleSaveDTO dto) {
        CategorySubModule entity = dto.getId() == null ? new CategorySubModule() : categorySubModuleMapper.selectById(dto.getId());
        entity.setModuleId(dto.getModuleId());
        entity.setSubModuleName(dto.getSubModuleName());
        entity.setSortOrder(dto.getSortOrder());
        if (dto.getId() == null) {
            categorySubModuleMapper.insert(entity);
        } else {
            categorySubModuleMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void deleteSubModule(Long id) {
        categorySubModuleMapper.deleteById(id);
    }

    private CategoryModuleVO toModuleVO(CategoryModule entity) {
        CategoryModuleVO vo = new CategoryModuleVO();
        vo.setId(entity.getId());
        vo.setGroupId(entity.getGroupId());
        vo.setModuleName(entity.getModuleName());
        vo.setImageUrl(entity.getImageUrl());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

    private CategorySubModuleVO toSubModuleVO(CategorySubModule entity) {
        CategorySubModuleVO vo = new CategorySubModuleVO();
        vo.setId(entity.getId());
        vo.setModuleId(entity.getModuleId());
        vo.setSubModuleName(entity.getSubModuleName());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }

}
