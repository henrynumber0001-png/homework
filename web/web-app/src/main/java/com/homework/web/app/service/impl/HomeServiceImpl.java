package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.HitPost;
import com.homework.model.entity.QuestionBank;
import com.homework.web.app.mapper.CategoryModuleMapper;
import com.homework.web.app.mapper.CategorySubModuleMapper;
import com.homework.web.app.mapper.HitPostMapper;
import com.homework.web.app.mapper.QuestionBankMapper;
import com.homework.web.app.service.HomeService;
import com.homework.web.app.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final QuestionBankMapper questionBankMapper;
    private final HitPostMapper hitPostMapper;
    private final CategoryModuleMapper categoryModuleMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;

    @Override
    public HomeVO getHome() {
        HomeVO vo = new HomeVO();
        vo.setInterviewHotBanks(listHotBanks(1L, 10));
        vo.setCertificationHotBanks(listHotBanks(2L, 10));
        vo.setInterviewHotSections(listHotSections(1L, 10));
        vo.setCertificationHotSections(listHotSections(2L, 10));
        vo.setHotHits(hitPostMapper.selectMaps(new QueryWrapper<HitPost>()
                .select("id", "user_id", "content", "tags_json", "like_count", "favorite_count", "repost_count", "created_time")
                .eq("post_status", 1)
                .eq("is_deleted", 0)
                .orderByDesc("like_count", "created_time")
                .last("LIMIT 10")));
        return vo;
    }

    private List<Map<String, Object>> listHotSections(Long groupId, int limitPerModule) {
        List<CategoryModule> modules = categoryModuleMapper.selectList(new QueryWrapper<CategoryModule>()
                .eq("group_id", groupId)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
        return modules.stream().map(module -> {
            List<CategorySubModule> subModules = listSubModules(module.getId());
            Map<String, Object> section = new HashMap<>();
            section.put("moduleId", module.getId());
            section.put("module", module.getModuleName());
            section.put("imageUrl", module.getImageUrl());
            section.put("banks", listHotBanksBySubModules(module, subModules, limitPerModule));
            return section;
        }).toList();
    }

    private List<Map<String, Object>> listHotBanks(Long groupId, int limit) {
        List<CategoryModule> modules = categoryModuleMapper.selectList(new QueryWrapper<CategoryModule>()
                .select("id")
                .eq("group_id", groupId)
                .eq("is_deleted", 0));
        List<Long> moduleIds = modules.stream().map(CategoryModule::getId).toList();
        if (moduleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return listHotBanksBySubModules(null, listSubModules(moduleIds), limit);
    }

    private List<Map<String, Object>> listHotBanksBySubModules(CategoryModule module,
                                                              List<CategorySubModule> subModules,
                                                              int limit) {
        if (subModules.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, CategorySubModule> subModuleById = subModules.stream()
                .collect(Collectors.toMap(CategorySubModule::getId, Function.identity(), (left, right) -> left));
        List<Long> subModuleIds = subModules.stream().map(CategorySubModule::getId).toList();
        return questionBankMapper.selectList(new QueryWrapper<QuestionBank>()
                        .in("sub_module_id", subModuleIds)
                        .eq("is_deleted", 0)
                        .orderByDesc("complete_user_count", "favorite_count", "view_count", "priority")
                        .orderByAsc("id")
                        .last("LIMIT " + Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(bank -> toHotBankItem(module, subModuleById.get(bank.getSubModuleId()), bank))
                .toList();
    }

    private List<CategorySubModule> listSubModules(Long moduleId) {
        return categorySubModuleMapper.selectList(new QueryWrapper<CategorySubModule>()
                .eq("module_id", moduleId)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
    }

    private List<CategorySubModule> listSubModules(List<Long> moduleIds) {
        if (moduleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return categorySubModuleMapper.selectList(new QueryWrapper<CategorySubModule>()
                .in("module_id", moduleIds)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
    }

    private Map<String, Object> toHotBankItem(CategoryModule module, CategorySubModule subModule, QuestionBank bank) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", bank.getId());
        item.put("moduleId", module == null ? null : module.getId());
        item.put("module", module == null ? null : module.getModuleName());
        item.put("subModuleId", bank.getSubModuleId());
        item.put("subModule", subModule == null ? null : subModule.getSubModuleName());
        item.put("name", bank.getBankName());
        item.put("count", bank.getCompleteUserCount());
        item.put("accuracy", bank.getAvgCorrectRate());
        item.put("isPremium", bank.getIsPremium());
        return item;
    }
}
