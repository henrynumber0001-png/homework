package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.AdminSortMode;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionActionDTO;
import com.homework.web.admin.dto.QuestionCreateDTO;
import com.homework.web.admin.dto.QuestionNoUpdateDTO;
import com.homework.web.admin.dto.QuestionUpdateDTO;
import com.homework.model.enums.QuestionAction;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionDetailVO;
import com.homework.web.admin.vo.QuestionNoUpdateResultVO;
import com.homework.web.admin.vo.QuestionRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    // 根据筛选条件查询指定题目
    public PageResult<QuestionRowVO> list(
            Long bankId,
            String keyword,
            QuestionInfoQuestionType questionType,
            Boolean released,
            Integer pageNum,
            Integer pageSize,
            AdminSortMode sortMode
        ) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        String normalizedKeyword = keyword == null ? null : keyword.trim();
        // 前端未传排序模式时，默认按更新时间降序。
        AdminSortMode selectedSortMode = sortMode == null ? AdminSortMode.UPDATED_TIME_DESC : sortMode;
        // 题目列表不支持题库专用的权重排序模式。
        if (selectedSortMode != AdminSortMode.UPDATED_TIME_DESC && selectedSortMode != AdminSortMode.QUESTION_NO_ASC) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        List<QuestionRowVO> rows = new ArrayList<>();

        //从这一步开始，就是把查询条件，根据管理员输入的情况，串起来
        if (groupType == GroupType.INTERVIEW) {
            LambdaQueryWrapper<InterviewQuestionInfo> query = new LambdaQueryWrapper<>();
            //bankId, questionType, released
            query.eq(InterviewQuestionInfo::getBankId, bankId)
                    .eq(questionType != null, InterviewQuestionInfo::getQuestionType, questionType)
                    .eq(released != null, InterviewQuestionInfo::getIsReleased, released);
            //keyword
            if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
                query.and(wrapper -> {
                    wrapper.like(InterviewQuestionInfo::getTitle, normalizedKeyword);
                });
            }
            //排序方式
            if (selectedSortMode == AdminSortMode.QUESTION_NO_ASC) {
                query.orderByAsc(InterviewQuestionInfo::getQuestionNo)
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
                    .eq(questionType != null, CertificateQuestionInfo::getQuestionType, questionType)
                    .eq(released != null, CertificateQuestionInfo::getIsReleased, released);
            if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
                query.and(wrapper -> {
                    wrapper.like(CertificateQuestionInfo::getTitle, normalizedKeyword);
                });
            }
            if (selectedSortMode == AdminSortMode.QUESTION_NO_ASC) {
                query.orderByAsc(CertificateQuestionInfo::getQuestionNo)
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
        QuestionBank bank = bankMapper.selectForUpdate(bankId);
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
            // 新题使用当前最大序号加一，默认追加到题库末尾。
            question.setQuestionNo(interviewQuestionMapper.selectMaxQuestionNo(bankId) + 1);
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
            question.setQuestionNo(certificateQuestionMapper.selectMaxQuestionNo(bankId) + 1);
            question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            question.setVersion(0);
            certificateQuestionMapper.insert(question);
            questionId = question.getId();
        }

        if (bankMapper.bumpVersion(bankId, bank.getVersion()) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
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
            BeanUtils.copyProperties(question, before);

            boolean publishedContentChanged = !Objects.equals(question.getTitle(), normalizedTitle);
            if (Boolean.TRUE.equals(question.getIsReleased()) && publishedContentChanged && (dto.getReason() == null || dto.getReason().isBlank())) {
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
        QuestionAction action = dto.getAction();
        QuestionBank bank = action == QuestionAction.DELETE
                ? bankMapper.selectForUpdate(bankId)
                : bankMapper.selectById(bankId);
        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

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
                compactInterviewQuestionNosAfterDelete(bankId, question.getQuestionNo());
                bumpBankQuestionOrderVersion(bankId, bank.getVersion());
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
                compactCertificateQuestionNosAfterDelete(bankId, question.getQuestionNo());
                bumpBankQuestionOrderVersion(bankId, bank.getVersion());
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

    /** 把一道题移动到目标序号，中间题目自动顺移。 */
    @Transactional
    public QuestionNoUpdateResultVO updateQuestionNo(
            Long bankId,
            Long questionId,
            QuestionNoUpdateDTO dto
    ) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectForUpdate(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        if (!bank.getVersion().equals(dto.getBankQuestionOrderVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        int previousQuestionNo;
        int questionCount;
        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectActiveForUpdate(bankId, questionId);
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            previousQuestionNo = question.getQuestionNo();
            questionCount = interviewQuestionMapper.selectActiveQuestionCount(bankId);
        } else {
            CertificateQuestionInfo question = certificateQuestionMapper.selectActiveForUpdate(bankId, questionId);
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            previousQuestionNo = question.getQuestionNo();
            questionCount = certificateQuestionMapper.selectActiveQuestionCount(bankId);
        }

        int targetQuestionNo = dto.getQuestionNo();
        if (targetQuestionNo < 1 || targetQuestionNo > questionCount) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
        }

        if (targetQuestionNo != previousQuestionNo) {
            bumpBankQuestionOrderVersion(bankId, bank.getVersion());
            if (groupType == GroupType.INTERVIEW) {
                moveInterviewQuestionNo(bankId, questionId, previousQuestionNo, targetQuestionNo);
            } else {
                moveCertificateQuestionNo(bankId, questionId, previousQuestionNo, targetQuestionNo);
            }
            auditService.record(
                    "QUESTION",
                    "QUESTION_NO_UPDATE",
                    "QUESTION",
                    questionId,
                    dto.getReason(),
                    Map.of("questionNo", previousQuestionNo),
                    Map.of("questionNo", targetQuestionNo)
            );
        }
        QuestionBank updatedBank = bankMapper.selectById(bankId);

        QuestionNoUpdateResultVO result = new QuestionNoUpdateResultVO();
        result.setBankId(bankId);
        result.setQuestionId(questionId);
        result.setPreviousQuestionNo(previousQuestionNo);
        result.setQuestionNo(targetQuestionNo);
        result.setBankQuestionOrderVersion(updatedBank.getVersion());
        result.setUpdatedTime(updatedBank.getUpdatedTime());
        return result;
    }

    private void moveInterviewQuestionNo(Long bankId, Long questionId, int previousNo, int targetNo) {
        requireSingleRow(interviewQuestionMapper.parkQuestionNo(bankId, questionId, previousNo));
        int lower = Math.min(previousNo, targetNo);
        int upper = Math.max(previousNo, targetNo);
        if (targetNo < previousNo) {
            upper--;
        } else {
            lower++;
        }
        shiftInterviewQuestionNoRange(bankId, lower, upper, targetNo < previousNo ? 1 : -1);
        requireSingleRow(interviewQuestionMapper.placeQuestionNo(bankId, questionId, targetNo));
    }

    private void moveCertificateQuestionNo(Long bankId, Long questionId, int previousNo, int targetNo) {
        requireSingleRow(certificateQuestionMapper.parkQuestionNo(bankId, questionId, previousNo));
        int lower = Math.min(previousNo, targetNo);
        int upper = Math.max(previousNo, targetNo);
        if (targetNo < previousNo) {
            upper--;
        } else {
            lower++;
        }
        shiftCertificateQuestionNoRange(bankId, lower, upper, targetNo < previousNo ? 1 : -1);
        requireSingleRow(certificateQuestionMapper.placeQuestionNo(bankId, questionId, targetNo));
    }

    private void compactInterviewQuestionNosAfterDelete(Long bankId, int deletedQuestionNo) {
        int maxQuestionNo = interviewQuestionMapper.selectMaxQuestionNo(bankId);
        shiftInterviewQuestionNoRange(bankId, deletedQuestionNo + 1, maxQuestionNo, -1);
    }

    private void compactCertificateQuestionNosAfterDelete(Long bankId, int deletedQuestionNo) {
        int maxQuestionNo = certificateQuestionMapper.selectMaxQuestionNo(bankId);
        shiftCertificateQuestionNoRange(bankId, deletedQuestionNo + 1, maxQuestionNo, -1);
    }

    private void shiftInterviewQuestionNoRange(Long bankId, int lower, int upper, int delta) {
        if (lower > upper) {
            return;
        }
        int expectedRows = upper - lower + 1;
        int parkedRows = interviewQuestionMapper.negateQuestionNoRange(bankId, lower, upper);
        int restoredRows = interviewQuestionMapper.restoreQuestionNoRange(bankId, lower, upper, delta);
        if (parkedRows != expectedRows || restoredRows != expectedRows) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
        }
    }

    private void shiftCertificateQuestionNoRange(Long bankId, int lower, int upper, int delta) {
        if (lower > upper) {
            return;
        }
        int expectedRows = upper - lower + 1;
        int parkedRows = certificateQuestionMapper.negateQuestionNoRange(bankId, lower, upper);
        int restoredRows = certificateQuestionMapper.restoreQuestionNoRange(bankId, lower, upper, delta);
        if (parkedRows != expectedRows || restoredRows != expectedRows) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
        }
    }

    private void bumpBankQuestionOrderVersion(Long bankId, Integer version) {
        if (bankMapper.bumpVersion(bankId, version) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
    }

    private void requireSingleRow(int updatedRows) {
        if (updatedRows != 1) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
        }
    }
}
