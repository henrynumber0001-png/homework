package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionActionDTO;
import com.homework.web.admin.dto.QuestionCreateDTO;
import com.homework.web.admin.dto.QuestionOrderDTO;
import com.homework.web.admin.dto.QuestionUpdateDTO;
import com.homework.model.enums.QuestionAction;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionDetailVO;
import com.homework.web.admin.vo.QuestionOrderResultVO;
import com.homework.web.admin.vo.QuestionRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** 后台题目查询、写入、状态和题库内顺序管理。 */
@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private final QuestionBankMapper bankMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final CertificateQuestionMapper certificateQuestionMapper;
    private final AdminAccessService accessService;
    private final QuestionContentService contentService;
    private final QuestionImageService imageService;
    private final QuestionAssembler assembler;
    private final AdminAuditService auditService;

    /**
     * 查询指定题库中的题目，并按请求条件完成过滤、排序和分页。
     * 变更：原来先查 question_bank_question 再逐题查实体；现在直接按题目表 bank_id 查询。
     */
    public PageResult<QuestionRowVO> list(
            Long bankId,
            String keyword,
            QuestionInfoQuestionType questionType,
            Boolean released,
            Integer pageNum,
            Integer pageSize,
            String sortMode
        ) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        QuestionInfoQuestionType typeFilter = questionType;

        String normalizedKeyword = keyword == null ? null : keyword.trim();
        String normalizedSortMode = sortMode == null || sortMode.isBlank() ? "UPDATED_TIME_DESC" : sortMode.trim().toUpperCase(Locale.ROOT);
        if (!"UPDATED_TIME_DESC".equals(normalizedSortMode) && !"MANUAL_ORDER_ASC".equals(normalizedSortMode)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        List<QuestionRowVO> rows = new ArrayList<>();
        if (groupType == GroupType.INTERVIEW) {
            LambdaQueryWrapper<InterviewQuestionInfo> query = new LambdaQueryWrapper<>();
            query.eq(InterviewQuestionInfo::getBankId, bankId)
                    .eq(typeFilter != null, InterviewQuestionInfo::getQuestionType, typeFilter)
                    .eq(released != null, InterviewQuestionInfo::getIsReleased, released);
            if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
                query.and(wrapper -> {
                    wrapper.like(InterviewQuestionInfo::getTitle, normalizedKeyword);
                    try {
                        wrapper.or().eq(InterviewQuestionInfo::getId, Long.valueOf(normalizedKeyword));
                    } catch (NumberFormatException ignored) {
                        // 变更：非数字关键词只匹配题干，避免无效 ID 转换中断查询。
                    }
                });
            }
            if ("MANUAL_ORDER_ASC".equals(normalizedSortMode)) {
                query.orderByAsc(InterviewQuestionInfo::getSortOrder)
                        .orderByAsc(InterviewQuestionInfo::getId);
            } else {
                query.orderByDesc(InterviewQuestionInfo::getUpdatedTime)
                        .orderByDesc(InterviewQuestionInfo::getId);
            }
            List<InterviewQuestionInfo> questions = interviewQuestionMapper.selectList(query);
            for (InterviewQuestionInfo question : questions) {
                rows.add(assembler.toRow(question));
            }
        } else {
            LambdaQueryWrapper<CertificateQuestionInfo> query = new LambdaQueryWrapper<>();
            query.eq(CertificateQuestionInfo::getBankId, bankId)
                    .eq(typeFilter != null, CertificateQuestionInfo::getQuestionType, typeFilter)
                    .eq(released != null, CertificateQuestionInfo::getIsReleased, released);
            if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
                query.and(wrapper -> {
                    wrapper.like(CertificateQuestionInfo::getTitle, normalizedKeyword);
                    try {
                        wrapper.or().eq(CertificateQuestionInfo::getId, Long.valueOf(normalizedKeyword));
                    } catch (NumberFormatException ignored) {
                        // 变更：非数字关键词只匹配题干，避免无效 ID 转换中断查询。
                    }
                });
            }
            if ("MANUAL_ORDER_ASC".equals(normalizedSortMode)) {
                query.orderByAsc(CertificateQuestionInfo::getSortOrder)
                        .orderByAsc(CertificateQuestionInfo::getId);
            } else {
                query.orderByDesc(CertificateQuestionInfo::getUpdatedTime)
                        .orderByDesc(CertificateQuestionInfo::getId);
            }
            List<CertificateQuestionInfo> questions = certificateQuestionMapper.selectList(query);
            for (CertificateQuestionInfo question : questions) {
                rows.add(assembler.toRow(question));
            }
        }

        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        int from = Math.min((normalizedPage - 1) * normalizedSize, rows.size());
        int to = Math.min(from + normalizedSize, rows.size());

        PageResult<QuestionRowVO> result = new PageResult<>();
        result.setRecords(rows.subList(from, to));
        result.setTotal(rows.size());
        result.setPageNum(normalizedPage);
        result.setPageSize(normalizedSize);
        return result;
    }

    /**
     * 查询某道题目的完整详情。
     * 变更：原来先校验关系记录，现在直接使用 bank_id + questionId 防止跨题库访问。
     */
    public QuestionDetailVO get(Long bankId, Long questionId) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectOne(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getId, questionId)
            );
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            return assembler.toDetail(groupType, question);
        }
        CertificateQuestionInfo question = certificateQuestionMapper.selectOne(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .eq(CertificateQuestionInfo::getBankId, bankId)
                        .eq(CertificateQuestionInfo::getId, questionId)
        );
        if (question == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }
        return assembler.toDetail(groupType, question);
    }

    /**
     * 在指定题库中创建题目。
     * 变更：不再创建关系记录，bankId 和追加顺序直接写入新题目。
     */
    @Transactional
    public QuestionDetailVO create(Long bankId, QuestionCreateDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        QuestionInfoQuestionType questionType = contentService.parseAndValidate(
                groupType,
                dto.getQuestionType(),
                dto.getOptions(),
                dto.getCorrectAnswers()
        );
        String normalizedTitle = dto.getTitle().trim();
        requireUniqueTitle(bankId, groupType, normalizedTitle, null);
        Long questionId;

        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = new InterviewQuestionInfo();
            question.setBankId(bankId);
            question.setTitle(normalizedTitle);
            question.setAnalysis(dto.getAnalysis());
            question.setQuestionType(questionType);
            question.setIsReleased(false);
            question.setCreateAdminId(AdminContext.getAdminId());
            // 变更：新题默认追加到当前题库末尾，使用 10 的间隔方便人工理解。
            question.setSortOrder(interviewQuestionMapper.selectMaxSortOrder(bankId) + 10);
            question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            question.setVersion(0);
            interviewQuestionMapper.insert(question);
            questionId = question.getId();
        } else {
            CertificateQuestionInfo question = new CertificateQuestionInfo();
            question.setBankId(bankId);
            question.setTitle(normalizedTitle);
            question.setAnalysis(dto.getAnalysis());
            question.setQuestionType(questionType);
            question.setOptions(contentService.toOptionContents(dto.getOptions()));
            question.setCorrectAnswer(contentService.toCorrectAnswerContents(
                    dto.getOptions(),
                    dto.getCorrectAnswers()
            ));
            question.setIsReleased(false);
            question.setCreateAdminId(AdminContext.getAdminId());
            // 变更：认证题也直接在实体表中计算并保存题库内顺序。
            question.setSortOrder(certificateQuestionMapper.selectMaxSortOrder(bankId) + 10);
            question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            question.setVersion(0);
            certificateQuestionMapper.insert(question);
            questionId = question.getId();
        }

        auditService.record("QUESTION", "CREATE", "QUESTION", questionId, "创建题目", null, dto);
        return get(bankId, questionId);
    }

    /**
     * 更新指定题目的内容和可选图片。
     * 变更：一题只属于一个题库，因此删除共享题权限校验，只保留 bankId 归属校验。
     */
    @Transactional
    public QuestionDetailVO update(Long bankId, Long questionId, QuestionUpdateDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

        QuestionInfoQuestionType questionType = contentService.parseAndValidate(
                groupType,
                dto.getQuestionType(),
                dto.getOptions(),
                dto.getCorrectAnswers()
        );
        if (Boolean.TRUE.equals(dto.getRemoveImage())
                && dto.getImageObjectKey() != null && !dto.getImageObjectKey().isBlank()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        String normalizedTitle = dto.getTitle().trim();

        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectOne(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getId, questionId)
            );
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            if (!question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            requireUniqueTitle(bankId, groupType, normalizedTitle, questionId);

            InterviewQuestionInfo before = new InterviewQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, before);
            boolean publishedContentChanged = !Objects.equals(question.getTitle(), normalizedTitle);
            if (Boolean.TRUE.equals(question.getIsReleased())
                    && publishedContentChanged
                    && (dto.getReason() == null || dto.getReason().isBlank())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            question.setQuestionType(questionType);
            question.setTitle(normalizedTitle);
            question.setAnalysis(dto.getAnalysis());
            if (Boolean.TRUE.equals(dto.getRemoveImage())) {
                question.setImageObjectKey(null);
            } else if (dto.getImageObjectKey() != null && !dto.getImageObjectKey().isBlank()) {
                question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            }
            if (interviewQuestionMapper.updateById(question) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            auditService.record("QUESTION", "UPDATE", "QUESTION", questionId, dto.getReason(), before, question);
        } else {
            CertificateQuestionInfo question = certificateQuestionMapper.selectOne(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bankId)
                            .eq(CertificateQuestionInfo::getId, questionId)
            );
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            if (!question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            requireUniqueTitle(bankId, groupType, normalizedTitle, questionId);

            CertificateQuestionInfo before = new CertificateQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, before);
            List<String> optionContents = contentService.toOptionContents(dto.getOptions());
            List<String> correctAnswerContents = contentService.toCorrectAnswerContents(
                    dto.getOptions(),
                    dto.getCorrectAnswers()
            );
            boolean publishedContentChanged = !Objects.equals(question.getTitle(), normalizedTitle)
                    || !Objects.equals(question.getOptions(), optionContents)
                    || !Objects.equals(question.getCorrectAnswer(), correctAnswerContents);
            if (Boolean.TRUE.equals(question.getIsReleased())
                    && publishedContentChanged
                    && (dto.getReason() == null || dto.getReason().isBlank())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            question.setQuestionType(questionType);
            question.setTitle(normalizedTitle);
            question.setAnalysis(dto.getAnalysis());
            question.setOptions(optionContents);
            question.setCorrectAnswer(correctAnswerContents);
            if (Boolean.TRUE.equals(dto.getRemoveImage())) {
                question.setImageObjectKey(null);
            } else if (dto.getImageObjectKey() != null && !dto.getImageObjectKey().isBlank()) {
                question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            }
            if (certificateQuestionMapper.updateById(question) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            auditService.record("QUESTION", "UPDATE", "QUESTION", questionId, dto.getReason(), before, question);
        }
        return get(bankId, questionId);
    }

    /** 校验同一题库的未删除题目中不存在相同题干。 */
    private void requireUniqueTitle(
            Long bankId,
            GroupType groupType,
            String title,
            Long excludedQuestionId
    ) {
        long sameTitle;
        if (groupType == GroupType.INTERVIEW) {
            sameTitle = interviewQuestionMapper.selectCount(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getTitle, title)
                            .ne(excludedQuestionId != null, InterviewQuestionInfo::getId, excludedQuestionId)
            );
        } else {
            sameTitle = certificateQuestionMapper.selectCount(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bankId)
                            .eq(CertificateQuestionInfo::getTitle, title)
                            .ne(excludedQuestionId != null, CertificateQuestionInfo::getId, excludedQuestionId)
            );
        }
        // BaseMapper 会自动过滤 is_deleted = 1，因此历史删除记录不会阻止重新创建同题干题目。
        if (sameTitle > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TITLE_CONFLICT);
        }
    }

    /**
     * 执行题目发布、下线或逻辑删除。
     * 变更：删除共享引用限制；管理端不再提供题目恢复能力。
     */
    @Transactional
    public ActionResultVO action(Long bankId, Long questionId, QuestionActionDTO dto) {
        accessService.requireBank(bankId);
        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

        QuestionAction action = dto.getAction();
        Object before;
        Object updated;

        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectOne(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getId, questionId)
            );
            if (question == null || !question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(question == null
                        ? ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND
                        : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            InterviewQuestionInfo beforeQuestion = new InterviewQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, beforeQuestion);
            before = beforeQuestion;

            if (action == QuestionAction.PUBLISH) {
                accessService.requirePermission("question:publish");
                if (Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(true);
                if (interviewQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.OFFLINE) {
                accessService.requirePermission("question:publish");
                if (!Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(false);
                if (interviewQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.DELETE) {
                accessService.requirePermission("question:delete");
                if (interviewQuestionMapper.logicalDelete(bankId, questionId, dto.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            updated = interviewQuestionMapper.selectIncludingDeleted(bankId, questionId);
        } else {
            CertificateQuestionInfo question = certificateQuestionMapper.selectOne(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bankId)
                            .eq(CertificateQuestionInfo::getId, questionId)
            );
            if (question == null || !question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(question == null
                        ? ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND
                        : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            CertificateQuestionInfo beforeQuestion = new CertificateQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, beforeQuestion);
            before = beforeQuestion;

            if (action == QuestionAction.PUBLISH) {
                accessService.requirePermission("question:publish");
                if (Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(true);
                if (certificateQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.OFFLINE) {
                accessService.requirePermission("question:publish");
                if (!Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(false);
                if (certificateQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.DELETE) {
                accessService.requirePermission("question:delete");
                if (certificateQuestionMapper.logicalDelete(bankId, questionId, dto.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            updated = certificateQuestionMapper.selectIncludingDeleted(bankId, questionId);
        }

        auditService.record("QUESTION", action.name(), "QUESTION", questionId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(questionId);
        result.setAction(action.getValue());
        if (updated instanceof InterviewQuestionInfo question) {
            result.setStatus(Boolean.TRUE.equals(question.getDeleted())
                    ? QuestionBankStatus.DELETED.getValue()
                    : Boolean.TRUE.equals(question.getIsReleased())
                    ? QuestionBankStatus.PUBLISHED.getValue()
                    : QuestionBankStatus.OFFLINE.getValue());
            result.setVersion(question.getVersion());
            result.setUpdatedTime(question.getUpdatedTime());
        } else if (updated instanceof CertificateQuestionInfo question) {
            result.setStatus(Boolean.TRUE.equals(question.getDeleted())
                    ? QuestionBankStatus.DELETED.getValue()
                    : Boolean.TRUE.equals(question.getIsReleased())
                    ? QuestionBankStatus.PUBLISHED.getValue()
                    : QuestionBankStatus.OFFLINE.getValue());
            result.setVersion(question.getVersion());
            result.setUpdatedTime(question.getUpdatedTime());
        }
        return result;
    }

    /**
     * 原子更新题库内全部有效题目的显示顺序。
     * 变更：原来更新关系表，现在直接把题目表顺序重排为 10、20、30……
     */
    @Transactional
    public QuestionOrderResultVO updateOrder(Long bankId, QuestionOrderDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        List<Long> activeQuestionIds;
        if (groupType == GroupType.INTERVIEW) {
            activeQuestionIds = interviewQuestionMapper.selectList(
                            new LambdaQueryWrapper<InterviewQuestionInfo>()
                                    .eq(InterviewQuestionInfo::getBankId, bankId)
                    ).stream()
                    .map(InterviewQuestionInfo::getId)
                    .toList();
        } else {
            activeQuestionIds = certificateQuestionMapper.selectList(
                            new LambdaQueryWrapper<CertificateQuestionInfo>()
                                    .eq(CertificateQuestionInfo::getBankId, bankId)
                    ).stream()
                    .map(CertificateQuestionInfo::getId)
                    .toList();
        }

        Set<Long> requestIds = new HashSet<>(dto.getQuestionIds());
        if (requestIds.size() != dto.getQuestionIds().size()
                || requestIds.size() != activeQuestionIds.size()
                || !requestIds.containsAll(activeQuestionIds)) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
        }
        if (!bank.getVersion().equals(dto.getBankQuestionOrderVersion())
                || bankMapper.bumpVersion(bankId, dto.getBankQuestionOrderVersion()) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        for (int index = 0; index < dto.getQuestionIds().size(); index++) {
            int sortOrder = (index + 1) * 10;
            int updatedRows;
            if (groupType == GroupType.INTERVIEW) {
                updatedRows = interviewQuestionMapper.updateSortOrder(
                        bankId,
                        dto.getQuestionIds().get(index),
                        sortOrder
                );
            } else {
                updatedRows = certificateQuestionMapper.updateSortOrder(
                        bankId,
                        dto.getQuestionIds().get(index),
                        sortOrder
                );
            }
            if (updatedRows != 1) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
            }
        }

        QuestionBank updatedBank = bankMapper.selectById(bankId);
        auditService.record(
                "QUESTION",
                "SORT",
                "QUESTION_BANK",
                bankId,
                dto.getReason(),
                activeQuestionIds,
                dto.getQuestionIds()
        );
        QuestionOrderResultVO result = new QuestionOrderResultVO();
        result.setBankId(bankId);
        result.setQuestionCount(dto.getQuestionIds().size());
        result.setBankQuestionOrderVersion(updatedBank.getVersion());
        result.setUpdatedTime(updatedBank.getUpdatedTime());
        return result;
    }
}
