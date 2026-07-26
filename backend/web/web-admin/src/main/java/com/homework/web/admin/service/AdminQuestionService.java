package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.BankDataScope;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionCreateDTO;
import com.homework.web.admin.dto.QuestionOrderDTO;
import com.homework.web.admin.dto.QuestionUpdateDTO;
import com.homework.web.admin.dto.ResourceActionDTO;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.mapper.QuestionBankQuestionMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionDetailVO;
import com.homework.web.admin.vo.QuestionOrderResultVO;
import com.homework.web.admin.vo.QuestionRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final QuestionBankQuestionMapper relationMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final CertificateQuestionMapper certificateQuestionMapper;
    private final AdminAccessService accessService;
    private final QuestionContentService contentService;
    private final QuestionImageService imageService;
    private final QuestionAssembler assembler;
    private final AdminAuditService auditService;

    public PageResult<QuestionRowVO> list(
            Long bankId,
            String keyword,
            String questionType,
            Boolean released,
            Boolean deleted,
            Integer pageNum,
            Integer pageSize,
            String sortBy,
            String sortDirection
    ) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectIncludingDeleted(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        QuestionInfoQuestionType typeFilter = null;
        if (questionType != null && !questionType.isBlank()) {
            try {
                typeFilter = QuestionInfoQuestionType.valueOf(questionType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID, exception);
            }
        }
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        List<QuestionRowVO> rows = new ArrayList<>();
        List<QuestionBankQuestion> relations = relationMapper.selectList(
                new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getBankId, bankId));
        for (QuestionBankQuestion relation : relations) {
            if (groupType == GroupType.INTERVIEW) {
                InterviewQuestionInfo question = interviewQuestionMapper.selectIncludingDeleted(relation.getQuestionId());
                if (question != null) {
                    rows.add(assembler.toRow(bankId, relation, question));
                }
            } else {
                CertificateQuestionInfo question = certificateQuestionMapper.selectIncludingDeleted(relation.getQuestionId());
                if (question != null) {
                    rows.add(assembler.toRow(bankId, relation, question));
                }
            }
        }
        QuestionInfoQuestionType finalTypeFilter = typeFilter;
        rows = rows.stream()
                .filter(row -> normalizedKeyword == null || normalizedKeyword.isBlank()
                        || row.getTitle().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || row.getId().toString().equals(normalizedKeyword))
                .filter(row -> finalTypeFilter == null || row.getQuestionType().equals(finalTypeFilter.name()))
                .filter(row -> released == null || row.getReleased().equals(released))
                .filter(row -> Boolean.TRUE.equals(deleted)
                        ? Boolean.TRUE.equals(row.getDeleted())
                        : !Boolean.TRUE.equals(row.getDeleted()))
                .toList();

        String normalizedSort = sortBy == null ? "BANK_ORDER" : sortBy.toUpperCase(Locale.ROOT);
        Comparator<QuestionRowVO> comparator = switch (normalizedSort) {
            case "CREATED_TIME" -> Comparator.comparing(
                    QuestionRowVO::getCreatedTime,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "UPDATED_TIME" -> Comparator.comparing(
                    QuestionRowVO::getUpdatedTime,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "QUESTION_ID" -> Comparator.comparing(QuestionRowVO::getId);
            default -> Comparator.comparing(
                    QuestionRowVO::getBankSortOrder,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        };
        if ("DESC".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        rows = rows.stream().sorted(comparator.thenComparing(QuestionRowVO::getId)).toList();
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

    public QuestionDetailVO get(Long bankId, Long questionId) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectIncludingDeleted(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        QuestionBankQuestion relation = relationMapper.selectOne(
                new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getBankId, bankId)
                        .eq(QuestionBankQuestion::getQuestionId, questionId));
        if (bank == null || groupType == null || relation == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }
        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectIncludingDeleted(questionId);
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            return assembler.toDetail(bankId, groupType, relation, question);
        }
        CertificateQuestionInfo question = certificateQuestionMapper.selectIncludingDeleted(questionId);
        if (question == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }
        return assembler.toDetail(bankId, groupType, relation, question);
    }

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
        Long questionId;
        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = new InterviewQuestionInfo();
            question.setTitle(dto.getTitle().trim());
            question.setAnalysis(dto.getAnalysis());
            question.setQuestionType(questionType);
            question.setIsReleased(false);
            question.setCreateAdminId(AdminContext.getAdminId());
            question.setSortOrder(0);
            question.setImageObjectKey(imageService.bind(dto.getImageUploadId()));
            question.setVersion(0);
            interviewQuestionMapper.insert(question);
            questionId = question.getId();
        } else {
            CertificateQuestionInfo question = new CertificateQuestionInfo();
            question.setTitle(dto.getTitle().trim());
            question.setAnalysis(dto.getAnalysis());
            question.setQuestionType(questionType);
            question.setOptions(contentService.toOptionContents(dto.getOptions()));
            question.setCorrectAnswer(contentService.toCorrectAnswerContents(
                    dto.getOptions(),
                    dto.getCorrectAnswers()
            ));
            question.setIsReleased(false);
            question.setCreateAdminId(AdminContext.getAdminId());
            question.setSortOrder(0);
            question.setImageObjectKey(imageService.bind(dto.getImageUploadId()));
            question.setVersion(0);
            certificateQuestionMapper.insert(question);
            questionId = question.getId();
        }
        QuestionBankQuestion relation = new QuestionBankQuestion();
        relation.setBankId(bankId);
        relation.setQuestionId(questionId);
        relation.setSortOrder(relationMapper.selectMaxSortOrder(bankId) + 10);
        relationMapper.insert(relation);
        auditService.record("QUESTION", "CREATE", "QUESTION", questionId, "创建题目", null, dto);
        return get(bankId, questionId);
    }

    @Transactional
    public QuestionDetailVO update(Long bankId, Long questionId, QuestionUpdateDTO dto) {
        accessService.requireBank(bankId);
        QuestionBankQuestion currentRelation = relationMapper.selectOne(
                new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getBankId, bankId)
                        .eq(QuestionBankQuestion::getQuestionId, questionId));
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (currentRelation == null || bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }
        requireAllReferencedBanks(questionId, groupType);
        QuestionInfoQuestionType questionType = contentService.parseAndValidate(
                groupType,
                dto.getQuestionType(),
                dto.getOptions(),
                dto.getCorrectAnswers()
        );
        if (Boolean.TRUE.equals(dto.getRemoveImage())
                && dto.getImageUploadId() != null && !dto.getImageUploadId().isBlank()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectById(questionId);
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            if (!question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            InterviewQuestionInfo before = new InterviewQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, before);
            boolean publishedContentChanged = !Objects.equals(question.getTitle(), dto.getTitle().trim());
            if (Boolean.TRUE.equals(question.getIsReleased())
                    && publishedContentChanged
                    && (dto.getReason() == null || dto.getReason().isBlank())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            question.setQuestionType(questionType);
            question.setTitle(dto.getTitle().trim());
            question.setAnalysis(dto.getAnalysis());
            if (Boolean.TRUE.equals(dto.getRemoveImage())) {
                question.setImageObjectKey(null);
            } else if (dto.getImageUploadId() != null && !dto.getImageUploadId().isBlank()) {
                question.setImageObjectKey(imageService.bind(dto.getImageUploadId()));
            }
            if (interviewQuestionMapper.updateById(question) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            auditService.record("QUESTION", "UPDATE", "QUESTION", questionId, dto.getReason(), before, question);
        } else {
            CertificateQuestionInfo question = certificateQuestionMapper.selectById(questionId);
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            if (!question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            CertificateQuestionInfo before = new CertificateQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, before);
            List<String> optionContents = contentService.toOptionContents(dto.getOptions());
            List<String> correctAnswerContents = contentService.toCorrectAnswerContents(
                    dto.getOptions(),
                    dto.getCorrectAnswers()
            );
            boolean publishedContentChanged = !Objects.equals(question.getTitle(), dto.getTitle().trim())
                    || !Objects.equals(question.getOptions(), optionContents)
                    || !Objects.equals(question.getCorrectAnswer(), correctAnswerContents);
            if (Boolean.TRUE.equals(question.getIsReleased())
                    && publishedContentChanged
                    && (dto.getReason() == null || dto.getReason().isBlank())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            question.setQuestionType(questionType);
            question.setTitle(dto.getTitle().trim());
            question.setAnalysis(dto.getAnalysis());
            question.setOptions(optionContents);
            question.setCorrectAnswer(correctAnswerContents);
            if (Boolean.TRUE.equals(dto.getRemoveImage())) {
                question.setImageObjectKey(null);
            } else if (dto.getImageUploadId() != null && !dto.getImageUploadId().isBlank()) {
                question.setImageObjectKey(imageService.bind(dto.getImageUploadId()));
            }
            if (certificateQuestionMapper.updateById(question) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            auditService.record("QUESTION", "UPDATE", "QUESTION", questionId, dto.getReason(), before, question);
        }
        return get(bankId, questionId);
    }

    @Transactional
    public ActionResultVO action(Long bankId, Long questionId, ResourceActionDTO dto) {
        accessService.requireBank(bankId);
        QuestionBankQuestion currentRelation = relationMapper.selectOne(
                new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getBankId, bankId)
                        .eq(QuestionBankQuestion::getQuestionId, questionId));
        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (currentRelation == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }
        requireAllReferencedBanks(questionId, groupType);
        String action = dto.getAction().trim().toUpperCase(Locale.ROOT);
        List<QuestionBankQuestion> references = relationMapper.selectQuestionRelations(questionId, groupType);
        Object before;
        Object updated;
        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectIncludingDeleted(questionId);
            if (question == null || !question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(question == null
                        ? ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND
                        : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            InterviewQuestionInfo beforeQuestion = new InterviewQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, beforeQuestion);
            before = beforeQuestion;
            if ("PUBLISH".equals(action)) {
                accessService.requirePermission("question:publish");
                if (Boolean.TRUE.equals(question.getDeleted()) || Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(true);
                if (interviewQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if ("OFFLINE".equals(action)) {
                accessService.requirePermission("question:publish");
                if (Boolean.TRUE.equals(question.getDeleted()) || !Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(false);
                if (interviewQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if ("DELETE".equals(action)) {
                accessService.requirePermission("question:delete");
                if (Boolean.TRUE.equals(question.getDeleted()) || references.size() > 1) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                if (interviewQuestionMapper.logicalDelete(questionId, dto.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if ("RESTORE".equals(action)) {
                accessService.requirePermission("question:delete");
                if (!Boolean.TRUE.equals(question.getDeleted())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                if (interviewQuestionMapper.restore(questionId, dto.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            updated = interviewQuestionMapper.selectIncludingDeleted(questionId);
        } else {
            CertificateQuestionInfo question = certificateQuestionMapper.selectIncludingDeleted(questionId);
            if (question == null || !question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(question == null
                        ? ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND
                        : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            CertificateQuestionInfo beforeQuestion = new CertificateQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, beforeQuestion);
            before = beforeQuestion;
            if ("PUBLISH".equals(action)) {
                accessService.requirePermission("question:publish");
                if (Boolean.TRUE.equals(question.getDeleted()) || Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(true);
                if (certificateQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if ("OFFLINE".equals(action)) {
                accessService.requirePermission("question:publish");
                if (Boolean.TRUE.equals(question.getDeleted()) || !Boolean.TRUE.equals(question.getIsReleased())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setIsReleased(false);
                if (certificateQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if ("DELETE".equals(action)) {
                accessService.requirePermission("question:delete");
                if (Boolean.TRUE.equals(question.getDeleted()) || references.size() > 1) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                if (certificateQuestionMapper.logicalDelete(questionId, dto.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if ("RESTORE".equals(action)) {
                accessService.requirePermission("question:delete");
                if (!Boolean.TRUE.equals(question.getDeleted())) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                if (certificateQuestionMapper.restore(questionId, dto.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            updated = certificateQuestionMapper.selectIncludingDeleted(questionId);
        }
        auditService.record("QUESTION", action, "QUESTION", questionId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(questionId);
        result.setAction(action);
        if (updated instanceof InterviewQuestionInfo question) {
            result.setStatus(Boolean.TRUE.equals(question.getDeleted())
                    ? "DELETED"
                    : Boolean.TRUE.equals(question.getIsReleased()) ? "PUBLISHED" : "OFFLINE");
            result.setVersion(question.getVersion());
            result.setUpdatedTime(question.getUpdatedTime());
        } else if (updated instanceof CertificateQuestionInfo question) {
            result.setStatus(Boolean.TRUE.equals(question.getDeleted())
                    ? "DELETED"
                    : Boolean.TRUE.equals(question.getIsReleased()) ? "PUBLISHED" : "OFFLINE");
            result.setVersion(question.getVersion());
            result.setUpdatedTime(question.getUpdatedTime());
        }
        return result;
    }

    @Transactional
    public QuestionOrderResultVO updateOrder(Long bankId, QuestionOrderDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        List<QuestionBankQuestion> relations = relationMapper.selectList(
                new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getBankId, bankId));
        List<Long> activeQuestionIds = new ArrayList<>();
        for (QuestionBankQuestion relation : relations) {
            boolean active;
            if (groupType == GroupType.INTERVIEW) {
                InterviewQuestionInfo question = interviewQuestionMapper.selectIncludingDeleted(relation.getQuestionId());
                active = question != null && !Boolean.TRUE.equals(question.getDeleted());
            } else {
                CertificateQuestionInfo question = certificateQuestionMapper.selectIncludingDeleted(relation.getQuestionId());
                active = question != null && !Boolean.TRUE.equals(question.getDeleted());
            }
            if (active) {
                activeQuestionIds.add(relation.getQuestionId());
            }
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
            relationMapper.updateSortOrder(bankId, dto.getQuestionIds().get(index), (index + 1) * 10);
        }
        QuestionBank updatedBank = bankMapper.selectById(bankId);
        auditService.record("QUESTION", "SORT", "QUESTION_BANK", bankId, dto.getReason(), activeQuestionIds, dto.getQuestionIds());
        QuestionOrderResultVO result = new QuestionOrderResultVO();
        result.setBankId(bankId);
        result.setQuestionCount(dto.getQuestionIds().size());
        result.setBankQuestionOrderVersion(updatedBank.getVersion());
        result.setUpdatedTime(updatedBank.getUpdatedTime());
        return result;
    }

    public void requireAllReferencedBanks(Long questionId, GroupType groupType) {
        if (AdminContext.get().getRole() == AdminRole.SUPER_ADMIN
                || AdminContext.get().getBankDataScope() == BankDataScope.ALL_BANKS) {
            return;
        }
        Set<Long> allowedBankIds = new HashSet<>(accessService.listAssignedBankIds(AdminContext.getAdminId()));
        boolean hiddenReference = relationMapper.selectQuestionRelations(questionId, groupType).stream()
                .anyMatch(relation -> !allowedBankIds.contains(relation.getBankId()));
        if (hiddenReference) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_SHARED_QUESTION_FORBIDDEN);
        }
    }
}
