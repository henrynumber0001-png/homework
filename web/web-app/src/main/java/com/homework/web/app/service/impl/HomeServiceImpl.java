package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.model.entity.CategoryDetail;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.HitPost;
import com.homework.model.entity.QuestionBank;
import com.homework.web.app.mapper.CategoryDetailMapper;
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
import java.util.LinkedHashMap;
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
    private final CategoryDetailMapper categoryDetailMapper;

    @Override
    public HomeVO getHome() {
        HomeVO vo = new HomeVO();
        vo.setInterviewHotBanks(listHotPrototypeBanks(1L, 10));
        vo.setCertificationHotBanks(listHotPrototypeBanks(2L, 10));
        vo.setInterviewHotSections(listHotPrototypeSections(1L, 10));
        vo.setCertificationHotSections(listHotPrototypeSections(2L, 10));
        vo.setHotHits(hitPostMapper.selectMaps(new QueryWrapper<HitPost>()
                .select("id", "user_id", "content", "tags_json", "like_count", "favorite_count", "repost_count", "created_time")
                .eq("post_status", 1)
                .eq("is_deleted", 0)
                .orderByDesc("like_count", "created_time")
                .last("LIMIT 10")));
        return vo;
    }

    private List<Map<String, Object>> listHotPrototypeSections(Long groupId, int limitPerModule) {
        List<CategoryModule> modules = categoryModuleMapper.selectList(new QueryWrapper<CategoryModule>()
                .eq("group_id", groupId)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
        return modules.stream().map(module -> {
            List<CategorySubModule> subModules = categorySubModuleMapper.selectList(new QueryWrapper<CategorySubModule>()
                    .eq("module_id", module.getId())
                    .eq("is_deleted", 0)
                    .orderByAsc("sort_order", "id"));
            List<Long> subModuleIds = subModules.stream().map(CategorySubModule::getId).toList();
            List<Map<String, Object>> banks = subModuleIds.isEmpty()
                    ? Collections.emptyList()
                    : categoryDetailMapper.selectList(new QueryWrapper<CategoryDetail>()
                            .in("sub_module_id", subModuleIds)
                            .eq("is_deleted", 0)
                            .orderByDesc("participant_count", "avg_correct_rate")
                            .last("LIMIT " + Math.max(1, Math.min(limitPerModule * 3, 60))))
                    .stream()
                    .collect(Collectors.toMap(CategoryDetail::getDetailName, Function.identity(), (left, right) -> left, LinkedHashMap::new))
                    .values()
                    .stream()
                    .limit(limitPerModule)
                    .map(detail -> toHotBankItem(module, detail, subModules))
                    .toList();
            Map<String, Object> section = new HashMap<>();
            section.put("moduleId", module.getId());
            section.put("module", module.getModuleName());
            section.put("displayIcon", module.getDisplayIcon());
            section.put("themeColor", module.getThemeColor());
            section.put("banks", banks);
            return section;
        }).toList();
    }

    private List<Map<String, Object>> listHotPrototypeBanks(Long groupId, int limit) {
        List<CategoryModule> modules = categoryModuleMapper.selectList(new QueryWrapper<CategoryModule>()
                .eq("group_id", groupId)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
        if (modules.isEmpty()) {
            return listHotBanks(groupId.intValue());
        }

        Map<Long, CategoryModule> moduleById = modules.stream()
                .collect(Collectors.toMap(CategoryModule::getId, Function.identity()));
        List<Long> moduleIds = modules.stream().map(CategoryModule::getId).toList();
        List<CategorySubModule> subModules = categorySubModuleMapper.selectList(new QueryWrapper<CategorySubModule>()
                .in("module_id", moduleIds)
                .eq("is_deleted", 0));
        if (subModules.isEmpty()) {
            return listHotBanks(groupId.intValue());
        }

        Map<Long, CategorySubModule> subModuleById = subModules.stream()
                .collect(Collectors.toMap(CategorySubModule::getId, Function.identity()));
        List<Long> subModuleIds = subModules.stream().map(CategorySubModule::getId).toList();
        List<CategoryDetail> details = categoryDetailMapper.selectList(new QueryWrapper<CategoryDetail>()
                .in("sub_module_id", subModuleIds)
                .eq("is_deleted", 0)
                .orderByDesc("participant_count", "avg_correct_rate")
                .last("LIMIT " + Math.max(1, Math.min(limit * 3, 60))));
        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashMap<String, CategoryDetail> uniqueDetails = new LinkedHashMap<>();
        details.forEach(detail -> uniqueDetails.putIfAbsent(detail.getDetailName(), detail));
        return uniqueDetails.values().stream().limit(limit).map(detail -> {
            CategorySubModule subModule = subModuleById.get(detail.getSubModuleId());
            CategoryModule module = subModule == null ? null : moduleById.get(subModule.getModuleId());
            return toHotBankItem(module, detail, subModules);
        }).toList();
    }

    private Map<String, Object> toHotBankItem(CategoryModule module, CategoryDetail detail, List<CategorySubModule> subModules) {
        CategorySubModule subModule = subModules.stream()
                .filter(item -> item.getId().equals(detail.getSubModuleId()))
                .findFirst()
                .orElse(null);
        Map<String, Object> item = new HashMap<>();
        item.put("id", detail.getId());
        item.put("moduleId", module == null ? null : module.getId());
        item.put("module", module == null ? null : module.getModuleName());
        item.put("subModuleId", detail.getSubModuleId());
        item.put("subModule", subModule == null ? null : subModule.getSubModuleName());
        item.put("name", detail.getDetailName());
        item.put("count", detail.getParticipantCount());
        item.put("accuracy", detail.getAvgCorrectRate());
        item.put("tagsJson", detail.getTagsJson());
        return item;
    }

    private List<Map<String, Object>> listHotBanks(Integer bankType) {
        return questionBankMapper.selectMaps(new QueryWrapper<QuestionBank>()
                .select("id", "name", "bank_type", "practice_user_count", "avg_correct_rate", "priority")
                .eq("bank_type", bankType)
                .eq("is_deleted", 0)
                .orderByDesc("practice_user_count", "favorite_count", "view_count", "priority")
                .last("LIMIT 10"));
    }
}
