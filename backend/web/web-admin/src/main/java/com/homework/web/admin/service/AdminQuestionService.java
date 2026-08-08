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
import com.homework.model.enums.QuestionInfoStatus;
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
            QuestionInfoStatus status,
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
            // 按题库、题型和题目状态组合筛选。
            query.eq(InterviewQuestionInfo::getBankId, bankId)
                    .eq(questionType != null, InterviewQuestionInfo::getQuestionType, questionType)
                    .eq(status != null, InterviewQuestionInfo::getStatus, status);
            //keyword
            if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
                query.and(wrapper -> {
                    wrapper.like(InterviewQuestionInfo::getTitle, normalizedKeyword);
                });
            }
            //排序方式(注意，这个排序方式是一定要有的，你选不选，都有一个设定，只不过默认是按更新时间)
            if (selectedSortMode == AdminSortMode.QUESTION_NO_ASC) {
                query.orderByAsc(InterviewQuestionInfo::getQuestionNo)
                        .orderByAsc(InterviewQuestionInfo::getId);
            } else {
                query.orderByDesc(InterviewQuestionInfo::getUpdatedTime)
                        .orderByDesc(InterviewQuestionInfo::getId);
            }

            //集中查询条件，返回questions行数据
            List<InterviewQuestionInfo> questions = interviewQuestionMapper.selectList(query);
            //组装并返回给前端
            for (InterviewQuestionInfo question : questions) {
                rows.add(assembler.toRow(question));
            }
        } else {
            LambdaQueryWrapper<CertificateQuestionInfo> query = new LambdaQueryWrapper<>();
            query.eq(CertificateQuestionInfo::getBankId, bankId)
                    .eq(questionType != null, CertificateQuestionInfo::getQuestionType, questionType)
                    .eq(status != null, CertificateQuestionInfo::getStatus, status);
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
        //通过bankId，拿到题目所属的模块大类
        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

        //如果是面试类，就去面试题目表里查这道题
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
        //如果是认证类，就去认证题目表里查这道题
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

        //给bank加悲观锁，当管理员A在创建这个题库中的题目时，其他管理员不能使用这个题库
        QuestionBank bank = bankMapper.selectForUpdate(bankId);

        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        //获取要创建题目的类型
        QuestionInfoQuestionType questionType = dto.getQuestionType();

        //验证管理员创建题目所输入的选项、正确答案是否合规
        contentService.validateQuestionCreation(
                groupType,
                questionType, //如果是ESSAY，直接返回return，因为没有选项和正确答案
                dto.getOptions(),
                dto.getCorrectAnswerKeys()
        );

        String normalizedTitle = dto.getTitle().trim();
        Long questionId;

        //检查未删除题目中是否有重复题干
        if (groupType == GroupType.INTERVIEW) {
            long sameTitle = interviewQuestionMapper.selectCount(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getTitle, normalizedTitle)
            );// BaseMapper 会自动过滤 is_deleted = 1，历史删除记录不会阻止重新创建同题干题目。

            if (sameTitle > 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TITLE_CONFLICT);
            }

            //没有重复题干，选项、答案都通过格式验证，题库能找到对应的题库大类，那么就开始保存题目到对应的数据库表
            InterviewQuestionInfo question = new InterviewQuestionInfo();
            question.setBankId(bankId);
            question.setTitle(normalizedTitle);
            question.setAnalysis(dto.getAnalysis());
            question.setQuestionType(questionType);
            question.setStatus(QuestionInfoStatus.DRAFT);
            question.setCreateAdminId(AdminContext.getAdminId());
            // 新题使用当前最大序号加一，默认追加到题库末尾。
            question.setQuestionNo(interviewQuestionMapper.selectMaxQuestionNo(bankId) + 1);

            //调用 QuestionImageService.bind() 方法，把前端传过来的图片的临时 objectKey 改名为 正式 objectKey
            //然后再复制正式 objectKey 到存储桶中替换（删除）临时 objectKey，永久存储
            //最后再把正式 objectKey 存入数据库表
            question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            question.setVersion(0);
            interviewQuestionMapper.insert(question);
            questionId = question.getId();
        } else {
            long sameTitle = certificateQuestionMapper.selectCount(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bankId)
                            .eq(CertificateQuestionInfo::getTitle, normalizedTitle)
            );
            if (sameTitle > 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TITLE_CONFLICT);
            }

            CertificateQuestionInfo question = new CertificateQuestionInfo();
            question.setBankId(bankId);
            question.setTitle(normalizedTitle);
            question.setAnalysis(dto.getAnalysis());
            question.setQuestionType(questionType);

            question.setOptions(contentService.toOptionContents(dto.getOptions()));
            question.setCorrectAnswer(contentService.toCorrectAnswerContents(dto.getOptions(), dto.getCorrectAnswerKeys()));

            question.setStatus(QuestionInfoStatus.DRAFT);
            question.setCreateAdminId(AdminContext.getAdminId());
            question.setQuestionNo(certificateQuestionMapper.selectMaxQuestionNo(bankId) + 1);
            question.setImageObjectKey(imageService.bind(dto.getImageObjectKey()));
            question.setVersion(0);
            certificateQuestionMapper.insert(question);
            questionId = question.getId();
        }

        if (bankMapper.updateVersion(bankId, bank.getVersion()) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        auditService.record("QUESTION", "CREATE", "QUESTION", questionId, "创建题目", null, dto);
        return get(bankId, questionId);
    }

    //目前前端在创建、更新成功后直接跳回题库工作台，没有实际使用返回类型QuestionDetailVO
    @Transactional
    public QuestionDetailVO update(Long bankId, Long questionId, QuestionUpdateDTO dto) {
        accessService.requireBank(bankId);

        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

        QuestionInfoQuestionType questionType = dto.getQuestionType();
        //更新题目，说明可能有修改，那么依然也要再次校验选项和答案的格式
        contentService.validateQuestionCreation(
                groupType,
                questionType,
                dto.getOptions(),
                dto.getCorrectAnswerKeys()
        );
        //一次修改请求中，不能同时要求“删除图片”和现有图片不为空
        if (Boolean.TRUE.equals(dto.getRemoveImage()) && dto.getImageObjectKey() != null && !dto.getImageObjectKey().isBlank()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //查看有没有重复的题目，因为管理员现在要执行修改操作，可能会有已经存在的题目
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
            long sameTitle = interviewQuestionMapper.selectCount(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getTitle, normalizedTitle)
                            .ne(InterviewQuestionInfo::getId, questionId)
            );
            if (sameTitle > 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TITLE_CONFLICT);
            }

            InterviewQuestionInfo before = new InterviewQuestionInfo();
            BeanUtils.copyProperties(question, before);

            boolean publishedContentChanged = !Objects.equals(question.getTitle(), normalizedTitle);
            //如果题目已经发布，且题干修改了，但没有写修改原因，那么就要报错
            //如果题目还是草稿状态，不写修改原因也没关系，因为这就是草稿的意义。但发布的题目必须严谨。
            if (question.getStatus() == QuestionInfoStatus.PUBLISHED && publishedContentChanged && (dto.getReason() == null || dto.getReason().isBlank())) {
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
            long sameTitle = certificateQuestionMapper.selectCount(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bankId)
                            .eq(CertificateQuestionInfo::getTitle, normalizedTitle)
                            .ne(CertificateQuestionInfo::getId, questionId)
            );
            if (sameTitle > 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TITLE_CONFLICT);
            }

            CertificateQuestionInfo before = new CertificateQuestionInfo();
            org.springframework.beans.BeanUtils.copyProperties(question, before);
            List<String> optionContents = contentService.toOptionContents(dto.getOptions());
            List<String> correctAnswerContents = contentService.toCorrectAnswerContents(
                    dto.getOptions(),
                    dto.getCorrectAnswerKeys()
            );
            boolean publishedContentChanged = !Objects.equals(question.getTitle(), normalizedTitle)
                    || !Objects.equals(question.getOptions(), optionContents)
                    || !Objects.equals(question.getCorrectAnswer(), correctAnswerContents);
            if (question.getStatus() == QuestionInfoStatus.PUBLISHED
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

    /**
     * 执行题目发布、下线或逻辑删除。
     * 变更：删除共享引用限制；管理端不再提供题目恢复能力。
     */
    @Transactional
    public ActionResultVO action(Long bankId, Long questionId, QuestionActionDTO dto) {
        accessService.requireBank(bankId);
        QuestionAction action = dto.getAction();
        QuestionBank bank = action == QuestionAction.DELETE ? bankMapper.selectForUpdate(bankId) : bankMapper.selectById(bankId);

        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
        }

        //为什么要用Object？
        //因为只有Object可以兼容InterviewQuestionInfo & CertificateQuestionInfo
        Object before;
        Object updated;

        if (groupType == GroupType.INTERVIEW) {
            InterviewQuestionInfo question = interviewQuestionMapper.selectOne(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bankId)
                            .eq(InterviewQuestionInfo::getId, questionId)
            );
            if (question == null || !question.getVersion().equals(dto.getVersion())) {
                throw new HomeworkException(question == null ? ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND : ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            InterviewQuestionInfo beforeQuestion = new InterviewQuestionInfo();
            BeanUtils.copyProperties(question, beforeQuestion);
            before = beforeQuestion;

            if (action == QuestionAction.PUBLISH) {
                //查询管理员是否有发布题目的权限
                accessService.requirePermission("question:publish");
                if (question.getStatus() == QuestionInfoStatus.PUBLISHED) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setStatus(QuestionInfoStatus.PUBLISHED);
                //乐观锁出问题了，需要刷新页面
                if (interviewQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.OFFLINE) {
                accessService.requirePermission("question:publish");
                if (question.getStatus() != QuestionInfoStatus.PUBLISHED) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setStatus(QuestionInfoStatus.OFFLINE);
                //乐观锁防止并发
                if (interviewQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.DELETE) { //草稿,已发布,已下架的题目都可以删除
                accessService.requirePermission("question:delete");

                // 删除题目后，它原来的序号会形成空缺。例如删除第 3 题后，原第 4～8 题都要向前移动一位。
                // 先获取需要删除的题目编号
                int deletedQuestionNo = question.getQuestionNo();
                // 查询最大题目编号
                int maxQuestionNo = interviewQuestionMapper.selectMaxQuestionNo(bankId);
                // 设定需要删除的题目的下一题（从这道题开始，需要向上移动）
                int lowerQuestionNo = deletedQuestionNo + 1;
                // 如果下一题的序号都大于最后一题了，那就不需要任何的向上移动了
                if (lowerQuestionNo <= maxQuestionNo) {
                    // 应当向上移动的题目行数
                    int expectedRows = maxQuestionNo - deletedQuestionNo;
//                    int expectedRows = maxQuestionNo - lowerQuestionNo + 1;

                    // 不能直接把 4～8 修改成 3～7，虽然MySQL内可以通过 ORDER BY 实现顺序更新，但这不代表其他数据库也支持UPDATE + ORDER BY
                    // UPDATE question_info SET question_no = question_no - 1 WHERE bank_id = #{bankId} AND #{question_no} BETWEEN lowerQuestionNo AND maxQuestionNo ORDER BY question_no ASC
                    // 最稳妥的解决方案是：先把受影响序号变成负数，再恢复成正数并减 1，最终得到连续的 1～N 序号。这样做的好处是，不会破坏bank_id + question_no的唯一索引
                    int temporaryRows = interviewQuestionMapper.negativeQuestionNoRange(bankId, lowerQuestionNo, maxQuestionNo);
                    // 这个算法，值得学习
                    // 4~8 -> -4~-8 -> -(-4) + (-1) ~ -(-8) + (-1) -> 3~7
                    int restoredRows = interviewQuestionMapper.restoreQuestionNoRange(bankId, lowerQuestionNo, maxQuestionNo, -1);
                    // 两次更新都必须准确影响预计数量的题目，否则说明题目序号已经不连续或被并发修改。


                    //这里一定要自定义一个删除方法，因为 乐观锁版本的自增，仅限update方法，delete不能实现
                    //而且自定义delete还可以写入 reason和version 字段
                    //一定要先删除，再将下面的题目上移
                    if (interviewQuestionMapper.logicalDelete(bankId, questionId, dto.getVersion(), QuestionInfoStatus.DELETED) == 0) {
                        throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                    }

                    if (temporaryRows != expectedRows || restoredRows != expectedRows) {
                        throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                    }
                }

                // 更新题目所在的题库的乐观锁版本
                if (bankMapper.updateVersion(bankId, bank.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            //发布、下架或删除操作完成后，重新从数据库查询这道题的最终状态，并保存到 updated 变量中，用于审计日志
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
            BeanUtils.copyProperties(question, beforeQuestion);
            before = beforeQuestion;

            if (action == QuestionAction.PUBLISH) {
                accessService.requirePermission("question:publish");
                if (question.getStatus() == QuestionInfoStatus.PUBLISHED) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setStatus(QuestionInfoStatus.PUBLISHED);
                if (certificateQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.OFFLINE) {
                accessService.requirePermission("question:publish");
                if (question.getStatus() != QuestionInfoStatus.PUBLISHED) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_STATE_INVALID);
                }
                question.setStatus(QuestionInfoStatus.OFFLINE);
                //完成字段的赋值，要在数据库表中更新一下
                //依旧是乐观锁防并发
                if (certificateQuestionMapper.updateById(question) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else if (action == QuestionAction.DELETE) {
                accessService.requirePermission("question:delete");
                if (certificateQuestionMapper.logicalDelete(
                        bankId,
                        questionId,
                        dto.getVersion(),
                        QuestionInfoStatus.DELETED
                ) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }

                // 删除题目会留下一个序号空缺，需要把它后面的所有认证题向前移动一位。
                int deletedQuestionNo = question.getQuestionNo();
                int maxQuestionNo = certificateQuestionMapper.selectMaxQuestionNo(bankId);
                int lowerQuestionNo = deletedQuestionNo + 1;
                if (lowerQuestionNo <= maxQuestionNo) {
                    //先算出有多少行需要向上移动
                    int expectedRows = maxQuestionNo - deletedQuestionNo;

                    // 先转成负序号避开唯一索引冲突，再恢复成正序号并统一减 1。
                    int temporaryRows = certificateQuestionMapper.negativeQuestionNoRange(bankId, lowerQuestionNo, maxQuestionNo);
                    int restoredRows = certificateQuestionMapper.restoreQuestionNoRange(bankId, lowerQuestionNo, maxQuestionNo, -1);
                    if (temporaryRows != expectedRows || restoredRows != expectedRows) {
                        throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                    }
                }

                // 更新题目所在的题库的乐观锁版本
                if (bankMapper.updateVersion(bankId, bank.getVersion()) == 0) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
                }
            } else {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            //发布、下架或删除操作完成后，重新从数据库查询这道题的最终状态，并保存到 updated 变量中，用于审计日志
            updated = certificateQuestionMapper.selectIncludingDeleted(bankId, questionId);
        }

        auditService.record("QUESTION", action.name(), "QUESTION", questionId, dto.getReason(), before, updated);
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(questionId);
        result.setAction(action.getValue());

        // updated 可能是面试题，也可能是认证题。先判断实际类型，再读取对应实体的最新状态。
        if (updated instanceof InterviewQuestionInfo question) {
            // 状态已经由 action 分支写入题目表，直接返回数据库中的最终状态，不再临时推算。
            result.setStatus(question.getStatus().getValue());
            result.setVersion(question.getVersion());
            result.setUpdatedTime(question.getUpdatedTime());
        } else if (updated instanceof CertificateQuestionInfo question) {
            result.setStatus(question.getStatus().getValue());
            result.setVersion(question.getVersion());
            result.setUpdatedTime(question.getUpdatedTime());
        }
        return result;
    }

    /** 把一道题移动到目标序号，中间题目自动顺移。 */
    @Transactional
    public QuestionNoUpdateResultVO updateQuestionNo(Long bankId, Long questionId, QuestionNoUpdateDTO dto) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectForUpdate(bankId);
        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        //乐观锁防并发
        if (!bank.getVersion().equals(dto.getBankQuestionOrderVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        int previousQuestionNo;
        int questionCount;
        if (groupType == GroupType.INTERVIEW) {

            //添加悲观锁，查要调整顺序的题目
            InterviewQuestionInfo question = interviewQuestionMapper.selectActiveForUpdate(bankId, questionId);
            if (question == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_NOT_FOUND);
            }
            //设置要调整顺序的题目的题目编号
            previousQuestionNo = question.getQuestionNo();
            //这个题库中有多少道题（只要没删除，就应该参与计数，因为草稿和下架题目也要参与排序，未来可能要上架，那么用户就会看到这道题，那么就要有顺序）
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
        //拟要修改的目标题号 小于1或大于题目总数，报错
        if (targetQuestionNo < 1 || targetQuestionNo > questionCount) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
        }

        if (targetQuestionNo != previousQuestionNo) {
            // 明确算出“哪些题目需要让路”，不再使用容易混淆的 lower/upper。
            // 例如：第 8 题改成第 3 题，第 3～7 题需要依次后移一位（序号 +1）。
            // 例如：第 3 题改成第 8 题，第 4～8 题需要依次前移一位（序号 -1）。
            int firstQuestionNoToMove;
            int lastQuestionNoToMove;
            int questionNoChange;
            if (targetQuestionNo < previousQuestionNo) { // 8 -> 3 的情况
                // 目标题向前移动。
                firstQuestionNoToMove = targetQuestionNo;
                lastQuestionNoToMove = previousQuestionNo - 1;

                //这个变量指的是 移动步幅
                questionNoChange = 1;
            } else { // 3 -> 8 的情况
                // 目标题向后移动。
                firstQuestionNoToMove = previousQuestionNo + 1;
                lastQuestionNoToMove = targetQuestionNo;

                //这个变量指的是 移动步幅
                questionNoChange = -1;
            }
            //计算需要移动的题目数量
            //当被移动题目和目标题目确定之后，不论方向是正是反，中间经过的题目总数是不变的
            int questionCountToMove = lastQuestionNoToMove - targetQuestionNo;

            if (groupType == GroupType.INTERVIEW) {
                // 第一步：先把被移动题目的序号暂存为 0，为其他题目让出位置。
                int parkedRowCount = interviewQuestionMapper.parkQuestionNo(bankId, questionId, previousQuestionNo);
                if (parkedRowCount != 1) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                }

                // 第二步：把需要让路的题号临时变成负数，避免调整时撞上题号唯一约束。
                int temporarilyMovedRowCount = interviewQuestionMapper.negativeQuestionNoRange(bankId, firstQuestionNoToMove, lastQuestionNoToMove);

                // 第三步：把负数恢复成正数，同时统一加 1 或减 1，完成题目顺移。
                // 这里需要注意的是：questionNoChange 是移动步幅，它是+1 还是 -1，完全取决于 被移动题目的移动方向
                int shiftedRowCount = interviewQuestionMapper.restoreQuestionNoRange(bankId, firstQuestionNoToMove, lastQuestionNoToMove, questionNoChange);
                if (temporarilyMovedRowCount != questionCountToMove || shiftedRowCount != questionCountToMove) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                }

                // 第四步：把暂存在序号 0 的目标题目放到管理员指定的新序号。
                int placedRowCount = interviewQuestionMapper.placeQuestionNo(bankId, questionId, targetQuestionNo);
                if (placedRowCount != 1) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                }
            } else {
                // 第一步：先把目标题目的序号暂存为 0，为其他题目让出位置。
                int parkedRowCount = certificateQuestionMapper.parkQuestionNo(bankId, questionId, previousQuestionNo);
                if (parkedRowCount != 1) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                }

                // 第二步：把需要让路的题号临时变成负数，避免调整时撞上题号唯一约束。
                int temporarilyMovedRowCount = certificateQuestionMapper.negativeQuestionNoRange(bankId, firstQuestionNoToMove, lastQuestionNoToMove);

                // 第三步：把负数恢复成正数，同时统一加 1 或减 1，完成题目顺移。
                int shiftedRowCount = certificateQuestionMapper.restoreQuestionNoRange(bankId, firstQuestionNoToMove, lastQuestionNoToMove, questionNoChange);
                if (temporarilyMovedRowCount != questionCountToMove || shiftedRowCount != questionCountToMove) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                }

                // 第四步：把暂存在序号 0 的目标题目放到管理员指定的新序号。
                int placedRowCount = certificateQuestionMapper.placeQuestionNo(bankId, questionId, targetQuestionNo);
                if (placedRowCount != 1) {
                    throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_ORDER_INVALID);
                }
            }

            // 题目序号全部调整成功后，再更新题库乐观锁版本
            // 当前方法有 @Transactional；如果版本更新失败，前面的序号修改也会一起回滚。
            if (bankMapper.updateVersion(bankId, bank.getVersion()) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

            auditService.record("QUESTION", "QUESTION_NO_UPDATE", "QUESTION", questionId, dto.getReason(),
                    Map.of("questionNo", previousQuestionNo), Map.of("questionNo", targetQuestionNo));
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
}
