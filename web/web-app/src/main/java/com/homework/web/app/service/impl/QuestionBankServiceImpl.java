package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.common.enums.EnumUtils;
import com.homework.common.enums.QuestionBankAccessType;
import com.homework.common.enums.QuestionBankBankStatus;
import com.homework.common.enums.QuestionBankBankType;
import com.homework.web.app.dto.BankQueryDTO;
import com.homework.web.app.dto.QuestionBankSaveDTO;
import com.homework.model.entity.CategoryDetail;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.QuestionBank;
import com.homework.model.entity.UserFavoriteBank;
import com.homework.web.app.mapper.CategoryDetailMapper;
import com.homework.web.app.mapper.CategorySubModuleMapper;
import com.homework.web.app.mapper.QuestionBankMapper;
import com.homework.web.app.mapper.UserFavoriteBankMapper;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.CategoryDetailVO;
import com.homework.web.app.vo.QuestionBankListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final CategoryDetailMapper categoryDetailMapper;
    private final CategorySubModuleMapper categorySubModuleMapper;
    private final QuestionBankMapper questionBankMapper;
    private final UserFavoriteBankMapper userFavoriteBankMapper;

    @Override
    public List<CategoryDetailVO> listPrototypeBanks(BankQueryDTO query) {
        List<Long> subModuleIds = resolveSubModuleIds(query);
        if (query.getModuleId() != null && subModuleIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<CategoryDetail> wrapper = new QueryWrapper<CategoryDetail>()
                .eq(query.getSubModuleId() != null, "sub_module_id", query.getSubModuleId())
                .in(query.getSubModuleId() == null && !subModuleIds.isEmpty(), "sub_module_id", subModuleIds)
                .like(StringUtils.hasText(query.getKeyword()), "detail_name", query.getKeyword())
                .eq("is_deleted", 0);
        if (isLatestSort(query.getSort())) {
            wrapper.orderByDesc("created_time").orderByAsc("id");
        } else {
            wrapper.orderByDesc("participant_count", "avg_correct_rate").orderByAsc("id");
        }
        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int size = query.getPageSize() == null ? 20 : Math.max(1, Math.min(query.getPageSize(), 100));
        wrapper.last("LIMIT " + Math.max(1, Math.min(pageNum * size * 3, 300)));
        LinkedHashMap<String, CategoryDetailVO> uniqueBanks = new LinkedHashMap<>();
        categoryDetailMapper.selectList(wrapper).stream()
                .map(this::toCategoryDetailVO)
                .forEach(vo -> uniqueBanks.putIfAbsent(uniqueBankKey(query, vo), vo));
        return uniqueBanks.values().stream()
                .skip((long) (pageNum - 1) * size)
                .limit(size)
                .toList();
    }

    @Override
    public List<QuestionBankListVO> listQuestionBanks(BankQueryDTO query) {
        QueryWrapper<QuestionBank> wrapper = new QueryWrapper<QuestionBank>()
                .like(StringUtils.hasText(query.getKeyword()), "name", query.getKeyword())
                .eq("is_deleted", 0);
        if (isLatestSort(query.getSort())) {
            wrapper.orderByDesc("created_time");
        } else {
            wrapper.orderByDesc("practice_user_count", "favorite_count", "view_count", "priority");
        }
        wrapper.last(limitClause(query));
        return questionBankMapper.selectList(wrapper).stream().map(this::toListVO).toList();
    }

    @Override
    public Long saveQuestionBank(QuestionBankSaveDTO dto) {
        QuestionBank entity = dto.getId() == null ? new QuestionBank() : questionBankMapper.selectById(dto.getId());
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setBankType(EnumUtils.fromValue(QuestionBankBankType.class, dto.getBankType()));
        entity.setDescription(dto.getDescription());
        entity.setCoverUrl(dto.getCoverUrl());
        entity.setPremium(dto.getPremium());
        entity.setAccessType(EnumUtils.fromValue(QuestionBankAccessType.class, dto.getAccessType()));
        entity.setBankStatus(EnumUtils.fromValue(QuestionBankBankStatus.class, dto.getBankStatus()));
        entity.setCreateUserId(dto.getCreateUserId());
        if (dto.getId() == null) {
            questionBankMapper.insert(entity);
        } else {
            questionBankMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void deleteQuestionBank(Long id) {
        questionBankMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> favoriteBank(Long userId, Long bankId) {
        UserFavoriteBank existing = userFavoriteBankMapper.selectOne(new QueryWrapper<UserFavoriteBank>()
                .eq("user_id", userId)
                .eq("bank_id", bankId)
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        boolean active = existing == null;
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (active) {
            UserFavoriteBank favorite = new UserFavoriteBank();
            favorite.setUserId(userId);
            favorite.setBankId(bankId);
            userFavoriteBankMapper.insert(favorite);
            if (bank != null) {
                bank.setFavoriteCount((bank.getFavoriteCount() == null ? 0 : bank.getFavoriteCount()) + 1);
                questionBankMapper.updateById(bank);
            }
        } else {
            userFavoriteBankMapper.deleteById(existing.getId());
            if (bank != null) {
                bank.setFavoriteCount(Math.max(0, (bank.getFavoriteCount() == null ? 0 : bank.getFavoriteCount()) - 1));
                questionBankMapper.updateById(bank);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("active", active);
        result.put("favoriteCount", bank == null ? null : bank.getFavoriteCount());
        return result;
    }

    private QuestionBankListVO toListVO(QuestionBank entity) {
        QuestionBankListVO vo = new QuestionBankListVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setBankType(entity.getBankType() == null ? null : entity.getBankType().getValue());
        vo.setQuestionCount(entity.getQuestionCount());
        vo.setPracticeUserCount(entity.getPracticeUserCount());
        vo.setAvgCorrectRate(entity.getAvgCorrectRate());
        vo.setFavoriteCount(entity.getFavoriteCount());
        vo.setViewCount(entity.getViewCount());
        vo.setPremium(entity.getPremium());
        return vo;
    }

    private List<Long> resolveSubModuleIds(BankQueryDTO query) {
        if (query.getSubModuleId() != null || query.getModuleId() == null) {
            return Collections.emptyList();
        }
        return categorySubModuleMapper.selectList(new QueryWrapper<CategorySubModule>()
                        .select("id")
                        .eq("module_id", query.getModuleId())
                        .eq("is_deleted", 0))
                .stream()
                .map(CategorySubModule::getId)
                .toList();
    }

    private CategoryDetailVO toCategoryDetailVO(CategoryDetail entity) {
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

    private String uniqueBankKey(BankQueryDTO query, CategoryDetailVO vo) {
        if (query.getSubModuleId() != null) {
            return vo.getSubModuleId() + ":" + vo.getDetailName();
        }
        return vo.getDetailName();
    }

    private boolean isLatestSort(String sort) {
        return "new".equalsIgnoreCase(sort) || "latest".equalsIgnoreCase(sort);
    }

    private String limitClause(BankQueryDTO query) {
        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 20 : Math.max(1, Math.min(query.getPageSize(), 100));
        int offset = (pageNum - 1) * pageSize;
        return "LIMIT " + offset + ", " + pageSize;
    }
}
