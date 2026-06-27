package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.web.app.dto.CategoryDetailSaveDTO;
import com.homework.web.app.dto.CategorySubModuleSaveDTO;
import com.homework.model.entity.CategoryDetail;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.web.app.mapper.CategoryDetailMapper;
import com.homework.web.app.mapper.CategoryModuleMapper;
import com.homework.web.app.mapper.CategorySubModuleMapper;
import com.homework.web.app.service.CategoryService;
import com.homework.web.app.vo.CategoryDetailVO;
import com.homework.web.app.vo.CategoryModuleVO;
import com.homework.web.app.vo.CategorySubModuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryModuleMapper categoryModuleMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;
    private final CategoryDetailMapper categoryDetailMapper;

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
    public List<CategoryDetailVO> listDetails(Long subModuleId) {
        return categoryDetailMapper.selectList(new QueryWrapper<CategoryDetail>()
                        .eq("sub_module_id", subModuleId)
                        .eq("is_deleted", 0)
                        .orderByAsc("sort_order", "id"))
                .stream()
                .map(this::toDetailVO)
                .collect(LinkedHashMap<String, CategoryDetailVO>::new,
                        (map, vo) -> map.putIfAbsent(vo.getDetailName(), vo),
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
    public Long saveDetail(CategoryDetailSaveDTO dto) {
        CategoryDetail entity = dto.getId() == null ? new CategoryDetail() : categoryDetailMapper.selectById(dto.getId());
        entity.setSubModuleId(dto.getSubModuleId());
        entity.setDetailName(dto.getDetailName());
        entity.setParticipantCount(dto.getParticipantCount());
        entity.setAvgCorrectRate(dto.getAvgCorrectRate());
        entity.setTagsJson(dto.getTagsJson());
        entity.setSortOrder(dto.getSortOrder());
        if (dto.getId() == null) {
            categoryDetailMapper.insert(entity);
        } else {
            categoryDetailMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void deleteSubModule(Long id) {
        categorySubModuleMapper.deleteById(id);
    }

    @Override
    public void deleteDetail(Long id) {
        categoryDetailMapper.deleteById(id);
    }

    private CategoryModuleVO toModuleVO(CategoryModule entity) {
        CategoryModuleVO vo = new CategoryModuleVO();
        vo.setId(entity.getId());
        vo.setGroupId(entity.getGroupId());
        vo.setModuleName(entity.getModuleName());
        vo.setDisplayIcon(entity.getDisplayIcon());
        vo.setThemeColor(entity.getThemeColor());
        vo.setThemeBgColor(entity.getThemeBgColor());
        vo.setThemeGradient(entity.getThemeGradient());
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

    private CategoryDetailVO toDetailVO(CategoryDetail entity) {
        CategoryDetailVO vo = new CategoryDetailVO();
        vo.setId(entity.getId());
        vo.setSubModuleId(entity.getSubModuleId());
        vo.setDetailName(entity.getDetailName());
        vo.setParticipantCount(entity.getParticipantCount());
        vo.setAvgCorrectRate(entity.getAvgCorrectRate());
        vo.setTagsJson(entity.getTagsJson());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }
}
