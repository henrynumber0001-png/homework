package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AdminBankScope;
import com.homework.model.entity.BankTag;
import com.homework.model.entity.CategoryGroup;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.BankDataScope;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionBankCreateDTO;
import com.homework.web.admin.dto.QuestionBankUpdateDTO;
import com.homework.web.admin.dto.ResourceActionDTO;
import com.homework.web.admin.mapper.AdminBankScopeMapper;
import com.homework.web.admin.mapper.BankTagMapper;
import com.homework.web.admin.mapper.CategoryGroupMapper;
import com.homework.web.admin.mapper.CategoryModuleMapper;
import com.homework.web.admin.mapper.CategorySubModuleMapper;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.mapper.QuestionBankQuestionMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionBankDetailVO;
import com.homework.web.admin.vo.QuestionBankRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** 后台题库查询、创建、编辑和状态管理。 */
@Service
@RequiredArgsConstructor
public class AdminQuestionBankService {

    private final QuestionBankMapper bankMapper;
    private final CategorySubModuleMapper subModuleMapper;
    private final CategoryModuleMapper moduleMapper;
    private final CategoryGroupMapper groupMapper;
    private final QuestionBankQuestionMapper relationMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final CertificateQuestionMapper certificateQuestionMapper;
    private final BankTagMapper bankTagMapper;
    private final AdminBankScopeMapper bankScopeMapper;
    private final AdminAccessService accessService;
    private final QuestionBankAssembler assembler;
    private final AdminAuditService auditService;

    public PageResult<QuestionBankRowVO> list(
            String keyword,
            GroupType groupType,
            Long moduleId,
            Long subModuleId,
            QuestionBankStatus status,
            Boolean deleted,
            Integer pageNum,
            Integer pageSize,
            String sortBy,
            String sortDirection
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        if (Boolean.TRUE.equals(deleted)) {
            accessService.requirePermission("bank:delete");
        }
        List<Long> allowedBankIds = accessService.listAssignedBankIds(AdminContext.getAdminId());

        List<Long> categorySubModuleIds = null;
        if (subModuleId != null) {
            categorySubModuleIds = List.of(subModuleId);
        } else if (moduleId != null) {
            categorySubModuleIds = subModuleMapper.selectList(
                            new LambdaQueryWrapper<CategorySubModule>()
                                    .eq(CategorySubModule::getModuleId, moduleId))
                    .stream()
                    .map(CategorySubModule::getId)
                    .toList();
        } else if (groupType != null) {
            List<Long> groupIds = groupMapper.selectList(new LambdaQueryWrapper<CategoryGroup>()
                            .eq(CategoryGroup::getGroupType, groupType))
                    .stream()
                    .map(CategoryGroup::getId)
                    .toList();
            List<Long> moduleIds = groupIds.isEmpty()
                    ? List.of()
                    : moduleMapper.selectList(new LambdaQueryWrapper<CategoryModule>()
                                    .in(CategoryModule::getGroupId, groupIds))
                            .stream()
                            .map(CategoryModule::getId)
                            .toList();
            categorySubModuleIds = moduleIds.isEmpty()
                    ? List.of()
                    : subModuleMapper.selectList(new LambdaQueryWrapper<CategorySubModule>()
                                    .in(CategorySubModule::getModuleId, moduleIds))
                            .stream()
                            .map(CategorySubModule::getId)
                            .toList();
        }

        boolean assignedOnly = AdminContext.get().getRole() != AdminRole.SUPER_ADMIN
                && AdminContext.get().getBankDataScope() == BankDataScope.ASSIGNED_BANKS;
        if ((assignedOnly && allowedBankIds.isEmpty())
                || (categorySubModuleIds != null && categorySubModuleIds.isEmpty())) {
            PageResult<QuestionBankRowVO> empty = new PageResult<>();
            empty.setRecords(List.of());
            empty.setTotal(0);
            empty.setPageNum(normalizedPage);
            empty.setPageSize(normalizedSize);
            return empty;
        }

        if (Boolean.TRUE.equals(deleted)) {
            List<Long> deletedCategorySubModuleIds = categorySubModuleIds;
            List<QuestionBank> filtered = bankMapper.selectDeletedList().stream()
                    .filter(bank -> !assignedOnly || allowedBankIds.contains(bank.getId()))
                    .filter(bank -> deletedCategorySubModuleIds == null
                            || deletedCategorySubModuleIds.contains(bank.getSubModuleId()))
                    .filter(bank -> status == null || bank.getStatus() == status)
                    .filter(bank -> keyword == null
                            || bank.getBankName().toLowerCase(Locale.ROOT)
                            .contains(keyword.trim().toLowerCase(Locale.ROOT))
                            || bank.getId().toString().equals(keyword.trim()))
                    .toList();
            int from = Math.min((normalizedPage - 1) * normalizedSize, filtered.size());
            int to = Math.min(from + normalizedSize, filtered.size());
            List<QuestionBankRowVO> records = filtered.subList(from, to).stream()
                    .map(assembler::toRow)
                    .toList();
            PageResult<QuestionBankRowVO> result = new PageResult<>();
            result.setRecords(records);
            result.setTotal(filtered.size());
            result.setPageNum(normalizedPage);
            result.setPageSize(normalizedSize);
            return result;
        }

        LambdaQueryWrapper<QuestionBank> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            query.and(wrapper -> {
                wrapper.like(QuestionBank::getBankName, normalizedKeyword);
                try {
                    wrapper.or().eq(QuestionBank::getId, Long.valueOf(normalizedKeyword));
                } catch (NumberFormatException ignored) {
                    // 非数字关键词只按名称搜索。
                }
            });
        }
        query.eq(status != null, QuestionBank::getStatus, status)
                .in(assignedOnly, QuestionBank::getId, allowedBankIds)
                .in(categorySubModuleIds != null, QuestionBank::getSubModuleId, categorySubModuleIds);
        boolean ascending = "ASC".equalsIgnoreCase(sortDirection);
        String normalizedSort = sortBy == null ? "UPDATED_TIME" : sortBy.toUpperCase(Locale.ROOT);
        switch (normalizedSort) {
            case "CREATED_TIME" -> query.orderBy(true, ascending, QuestionBank::getCreatedTime);
            case "PUBLISHED_TIME" -> query.orderBy(true, ascending, QuestionBank::getPublishedTime);
            case "PRIORITY" -> query.orderBy(true, ascending, QuestionBank::getPriority);
            case "VIEW_COUNT" -> query.orderBy(true, ascending, QuestionBank::getViewCount);
            case "COMPLETE_COUNT" -> query.orderBy(true, ascending, QuestionBank::getCompleteCount);
            default -> query.orderBy(true, ascending, QuestionBank::getUpdatedTime);
        }
        query.orderByDesc(QuestionBank::getId);

        Page<QuestionBank> page = bankMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<QuestionBankRowVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(assembler::toRow).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    public QuestionBankDetailVO get(Long bankId) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectIncludingDeleted(bankId);
        if (bank == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        return assembler.toDetail(bank);
    }

    @Transactional
    public QuestionBankDetailVO create(QuestionBankCreateDTO dto) {
        CategorySubModule subModule = subModuleMapper.selectById(dto.getSubModuleId());
        if (subModule == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }
        Long sameName = bankMapper.selectCount(new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getBankName, dto.getBankName().trim()));
        if (sameName > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NAME_CONFLICT);
        }

        QuestionBank bank = new QuestionBank();
        bank.setBankName(dto.getBankName().trim());
        bank.setSubModuleId(dto.getSubModuleId());
        bank.setCompleteCount(0);
        bank.setAvgCorrectRate(java.math.BigDecimal.ZERO);
        bank.setViewCount(0);
        bank.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        bank.setCreateAdminId(AdminContext.getAdminId());
        bank.setStatus(QuestionBankStatus.DRAFT);
        bank.setVersion(0);
        try {
            bankMapper.insert(bank);
        } catch (DuplicateKeyException exception) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NAME_CONFLICT, exception);
        }

        List<String> tags = dto.getTags() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(dto.getTags().stream()
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList()));
        for (String tagName : tags) {
            BankTag tag = new BankTag();
            tag.setBankId(bank.getId());
            tag.setTagName(tagName);
            bankTagMapper.insert(tag);
        }

        if (AdminContext.get().getRole() != AdminRole.SUPER_ADMIN
                && AdminContext.get().getBankDataScope() == BankDataScope.ASSIGNED_BANKS) {
            AdminBankScope scope = new AdminBankScope();
            scope.setAdminId(AdminContext.getAdminId());
            scope.setBankId(bank.getId());
            bankScopeMapper.insert(scope);
        }
        auditService.record("BANK", "CREATE", "QUESTION_BANK", bank.getId(), "创建题库", null, bank);
        return assembler.toDetail(bankMapper.selectById(bank.getId()));
    }

    @Transactional
    public QuestionBankDetailVO update(Long bankId, QuestionBankUpdateDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        if (bank == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        if (!bank.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        CategorySubModule targetSubModule = subModuleMapper.selectById(dto.getSubModuleId());
        if (targetSubModule == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }
        GroupType currentGroup = bankMapper.selectGroupType(bankId);
        CategoryModule targetModule = moduleMapper.selectById(targetSubModule.getModuleId());
        CategoryGroup targetGroup = targetModule == null ? null : groupMapper.selectById(targetModule.getGroupId());
        if (targetGroup == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }
        Long relationCount = relationMapper.selectCount(new LambdaQueryWrapper<QuestionBankQuestion>()
                .eq(QuestionBankQuestion::getBankId, bankId));
        if (relationCount > 0 && currentGroup != targetGroup.getGroupType()) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }
        Long sameName = bankMapper.selectCount(new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getBankName, dto.getBankName().trim())
                .ne(QuestionBank::getId, bankId));
        if (sameName > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NAME_CONFLICT);
        }

        QuestionBank before = new QuestionBank();
        org.springframework.beans.BeanUtils.copyProperties(bank, before);
        bank.setBankName(dto.getBankName().trim());
        bank.setSubModuleId(dto.getSubModuleId());
        bank.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        if (bankMapper.updateById(bank) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        bankTagMapper.delete(new LambdaQueryWrapper<BankTag>().eq(BankTag::getBankId, bankId));
        List<String> tags = dto.getTags() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(dto.getTags().stream()
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList()));
        for (String tagName : tags) {
            BankTag tag = new BankTag();
            tag.setBankId(bankId);
            tag.setTagName(tagName);
            bankTagMapper.insert(tag);
        }
        auditService.record("BANK", "UPDATE", "QUESTION_BANK", bankId, dto.getReason(), before, bank);
        return assembler.toDetail(bankMapper.selectById(bankId));
    }

    @Transactional
    public ActionResultVO action(Long bankId, ResourceActionDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectIncludingDeleted(bankId);
        if (bank == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        if (!bank.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        String action = dto.getAction().trim().toUpperCase(Locale.ROOT);
        QuestionBank before = new QuestionBank();
        org.springframework.beans.BeanUtils.copyProperties(bank, before);

        if ("PUBLISH".equals(action)) {
            accessService.requirePermission("bank:publish");
            if (Boolean.TRUE.equals(bank.getDeleted())
                    || (bank.getStatus() != QuestionBankStatus.DRAFT
                    && bank.getStatus() != QuestionBankStatus.OFFLINE)) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }
            List<Long> questionIds = relationMapper.selectList(
                            new LambdaQueryWrapper<QuestionBankQuestion>()
                                    .eq(QuestionBankQuestion::getBankId, bankId))
                    .stream()
                    .map(QuestionBankQuestion::getQuestionId)
                    .toList();
            long released = 0;
            GroupType groupType = bankMapper.selectGroupType(bankId);
            if (!questionIds.isEmpty() && groupType == GroupType.INTERVIEW) {
                released = interviewQuestionMapper.selectCount(
                        new LambdaQueryWrapper<InterviewQuestionInfo>()
                                .in(InterviewQuestionInfo::getId, questionIds)
                                .eq(InterviewQuestionInfo::getIsReleased, true));
            }
            if (!questionIds.isEmpty() && groupType == GroupType.CERTIFICATION) {
                released = certificateQuestionMapper.selectCount(
                        new LambdaQueryWrapper<CertificateQuestionInfo>()
                                .in(CertificateQuestionInfo::getId, questionIds)
                                .eq(CertificateQuestionInfo::getIsReleased, true));
            }
            if (released == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NO_RELEASED_QUESTION);
            }
            bank.setStatus(QuestionBankStatus.PUBLISHED);
            if (bank.getPublishedTime() == null) {
                bank.setPublishedTime(LocalDateTime.now());
            }
            if (bankMapper.updateById(bank) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
        } else if ("OFFLINE".equals(action)) {
            accessService.requirePermission("bank:publish");
            if (Boolean.TRUE.equals(bank.getDeleted()) || bank.getStatus() != QuestionBankStatus.PUBLISHED) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }
            bank.setStatus(QuestionBankStatus.OFFLINE);
            if (bankMapper.updateById(bank) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
        } else if ("DELETE".equals(action)) {
            accessService.requirePermission("bank:delete");
            if (Boolean.TRUE.equals(bank.getDeleted()) || bank.getStatus() == QuestionBankStatus.PUBLISHED) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }
            if (bankMapper.logicalDelete(bankId, dto.getReason(), dto.getVersion()) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
        } else if ("RESTORE".equals(action)) {
            accessService.requirePermission("bank:delete");
            if (!Boolean.TRUE.equals(bank.getDeleted())) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }
            if (bankMapper.restore(bankId, dto.getVersion()) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
        } else {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        QuestionBank updated = bankMapper.selectIncludingDeleted(bankId);
        auditService.record("BANK", action, "QUESTION_BANK", bankId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(bankId);
        result.setAction(action);
        result.setStatus(updated.getStatus().name());
        result.setVersion(updated.getVersion());
        result.setUpdatedTime(updated.getUpdatedTime());
        return result;
    }
}
