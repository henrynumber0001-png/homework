package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.CertificateExamAnswer;
import com.homework.model.entity.CertificateExamSession;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.enums.ExamSessionStatus;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.CertificateExamAnswerDTO;
import com.homework.web.app.mapper.CertificateExamAnswerMapper;
import com.homework.web.app.mapper.CertificateExamSessionMapper;
import com.homework.web.app.mapper.CertificateQuestionInfoMapper;
import com.homework.web.app.mapper.QuestionBankQuestionMapper;
import com.homework.web.app.service.CertificateExamService;
import com.homework.web.app.vo.BankFinishVO;
import com.homework.web.app.vo.CertificateExamQuestionVO;
import com.homework.web.app.vo.CertificateExamVO;
import com.homework.web.app.vo.CertificateQuestionReviewVO;
import com.homework.web.app.vo.QuestionCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证题库考试模式的业务实现。
 *
 * <p>这个类只负责考试模式，不负责练习模式。考试进行中的答案先保存在
 * certificate_exam_answer，提交后再根据这些临时答案统一判题。</p>
 */
@Service
@RequiredArgsConstructor
public class CertificateExamServiceImpl implements CertificateExamService {

    // 当前先把每场考试固定为 60 分钟；以后可以改成从题库配置表读取。
    private static final long EXAM_DURATION_MINUTES = 60L;

    // 操作考试场次表，例如创建考试、修改考试状态。
    private final CertificateExamSessionMapper certificateExamSessionMapper;

    // 操作考试临时答案表，例如保存用户当前选择。
    private final CertificateExamAnswerMapper certificateExamAnswerMapper;

    // 查询认证题目的标题、选项、正确答案和解析。
    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;

    // 查询题库与题目的关联关系，防止把其他题库的题混进考试。
    private final QuestionBankQuestionMapper questionBankQuestionMapper;

    /**
     * 开始一场新考试，或者恢复当前题库中尚未结束的考试。
     */
    @Transactional
    @Override
    public CertificateExamVO startOrResume(Long bankId) {

        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long userId = LoginUserHolder.getUserId();

        // 查询该用户在这个题库中【最近一场】仍标记为“进行中”的考试。
        CertificateExamSession activeSession = certificateExamSessionMapper.selectOne(
                new LambdaQueryWrapper<CertificateExamSession>()
                        .eq(CertificateExamSession::getUserId, userId)
                        .eq(CertificateExamSession::getBankId, bankId)
                        .eq(CertificateExamSession::getStatus, ExamSessionStatus.IN_PROGRESS)
                        .orderByDesc(CertificateExamSession::getId)
                        .last("LIMIT 1")
        );

        // 如果 CertificateExamSession 中的最新一条 activeSession 不为空，说明有进行中的考试。
        if (activeSession != null) {
            // 看看最新的这条 activeSession 是否已过期（isExpired == true）
            if (isExpired(activeSession)) { //如果过期了，把这条session的状态调整到 EXPIRED
                finishSession(activeSession, ExamSessionStatus.EXPIRED);
            }

            // 如果还没过期，返回原 session，继续答题。
            // 因此，刷新页面后题目顺序和用户选择不会改变。
            return buildExamVO(activeSession);
        }

        // 如果CertificateExamSession 中没有最新一条 activeSession，查询当前题库全部有效认证题。
        List<CertificateQuestionInfo> questionInfos = findReleasedQuestions(bankId);

        // 只在创建新场次时随机一次；随机后的结果马上保存到 questionOrder，今后只要进入这个 session，题目就按照这个 questionOrder 排列。
        Collections.shuffle(questionInfos);

        // questionOrder = verifiedQuestionIds
        List<Long> questionOrder = questionInfos.stream()
                .map(CertificateQuestionInfo::getId)
                .toList();

        // 获取一次当前时间，避免 startedAt 和 expiresAt 因两次取值产生细小偏差。
        LocalDateTime now = LocalDateTime.now();

        CertificateExamSession newSession = new CertificateExamSession();
        newSession.setUserId(userId);
        newSession.setBankId(bankId);
        // 保存本场考试的随机题序，之后恢复页面只认这个顺序。
        newSession.setQuestionOrder(questionOrder);
        newSession.setStartedAt(now);
        // 截止时间等于开始时间加 60 分钟；关闭浏览器不会暂停它。
        newSession.setExpiresAt(now.plusMinutes(EXAM_DURATION_MINUTES));
        newSession.setStatus(ExamSessionStatus.IN_PROGRESS);
        certificateExamSessionMapper.insert(newSession);

        return buildExamVO(newSession);//这个时候还没返回题目列表给前端呢，必须要调用buildExamVO之后
    }

    /**
     * 根据 sessionId 恢复某一场属于当前用户的考试。
     */
    @Transactional
    @Override
    public CertificateExamVO getSession(Long sessionId) {
        // sessionId 为空时无法确定要恢复哪一场考试。
        if (sessionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 查询时同时校验 userId，防止用户读取其他人的考试。
        CertificateExamSession session = getOwnedSession(sessionId);

        // 只有进行中的考试才需要检查是否刚刚超时。
        if (session.getStatus() == ExamSessionStatus.IN_PROGRESS && isExpired(session)) {
            // 超时后统一判题，并把状态改为 EXPIRED。
            finishSession(session, ExamSessionStatus.EXPIRED);
        }

        // 返回固定题序和已暂存的选择，供前端恢复页面。
        return buildExamVO(session);
    }

    /**
     * 覆盖保存用户对某一道题的临时选择。
     */
    @Transactional(noRollbackFor = HomeworkException.class)
    @Override
    public void saveAnswer(Long sessionId, Long questionId, CertificateExamAnswerDTO dto) {
        // 三个参数共同决定“哪场考试、哪道题、保存什么”，缺一不可。
        if (sessionId == null || questionId == null || dto == null || dto.getChosenOptions() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 查询并验证考试确实属于当前登录用户。
        CertificateExamSession session = getOwnedSession(sessionId);

        // 已提交、已超时或已放弃的考试都不能继续修改答案。
        ensureInProgress(session);

        // 截止时间已到时，先自动交卷，再通知前端考试已过期。
        if (isExpired(session)) {
            finishSession(session, ExamSessionStatus.EXPIRED);
            throw new HomeworkException(ResultCodeEnum.EXAM_EXPIRED);
        }

        // questionOrder 是本场考试允许作答的题目白名单。
        if (session.getQuestionOrder() == null || !session.getQuestionOrder().contains(questionId)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 从数据库读取真实题目，不能相信前端自己声明题型或选项范围。
        CertificateQuestionInfo questionInfo = certificateQuestionInfoMapper.selectById(questionId);

        // 题目被删除或题型不属于认证题时，当前考试数据已经不完整。
        if (questionInfo == null || !isCertificateQuestionType(questionInfo.getQuestionType())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 检查用户提交的每一个选项是否真实存在，并检查单选题数量规则。
        validateChosenOptions(questionInfo, dto.getChosenOptions());

        // 同一场考试的同一道题只允许有一条临时答案，因此先查询旧记录。
        CertificateExamAnswer savedAnswer = certificateExamAnswerMapper.selectOne(
                new LambdaQueryWrapper<CertificateExamAnswer>()
                        .eq(CertificateExamAnswer::getSessionId, sessionId)
                        .eq(CertificateExamAnswer::getQuestionId, questionId)
        );

        // 第一次选择这道题时，需要插入一条新记录。
        if (savedAnswer == null) {
            // 创建临时答案实体。
            CertificateExamAnswer newAnswer = new CertificateExamAnswer();

            // 关联到当前考试场次。
            newAnswer.setSessionId(sessionId);

            // 冗余保存 userId，方便后续按用户审计和查询。
            newAnswer.setUserId(session.getUserId());

            // 记录用户正在回答哪一道题。
            newAnswer.setQuestionId(questionId);

            // 保存用户当前的选择；空列表表示用户主动清空选择。
            newAnswer.setChosenOptions(new ArrayList<>(dto.getChosenOptions()));

            // 使用服务器时间记录最后作答时间。
            newAnswer.setAnsweredAt(LocalDateTime.now());

            // 把临时答案插入数据库。
            certificateExamAnswerMapper.insert(newAnswer);

            // 插入完成后即可结束本方法，不需要继续执行更新逻辑。
            return;
        }

        // 已经有记录时直接覆盖选择，保证每场考试每道题只有一个最新状态。
        savedAnswer.setChosenOptions(new ArrayList<>(dto.getChosenOptions()));

        // 每次修改答案都刷新最后作答时间。
        savedAnswer.setAnsweredAt(LocalDateTime.now());

        // 根据临时答案主键更新数据库。
        certificateExamAnswerMapper.updateById(savedAnswer);
    }

    /**
     * 主动提交试卷；重复请求时不会重复创建结果，而是重新计算并返回同一场结果。
     */
    @Transactional
    @Override
    public BankFinishVO submit(Long sessionId) {
        // sessionId 不能为空。
        if (sessionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 只能提交属于当前用户自己的考试。
        CertificateExamSession session = getOwnedSession(sessionId);

        // 放弃后的考试不允许再次提交。
        if (session.getStatus() == ExamSessionStatus.ABANDONED) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 已提交或已自动交卷时，直接返回已有场次对应的结算结果，保证接口幂等。
        if (session.getStatus() == ExamSessionStatus.SUBMITTED
                || session.getStatus() == ExamSessionStatus.EXPIRED) {
            return buildFinishVO(session);
        }

        // 用户在截止时间之后才点击提交时，应按超时交卷处理。
        ExamSessionStatus finalStatus = isExpired(session)
                ? ExamSessionStatus.EXPIRED
                : ExamSessionStatus.SUBMITTED;

        // 统一判题、保存统计结果并返回答案解析。
        return finishSession(session, finalStatus);
    }

    /**
     * 用户主动放弃考试。这里保留场次和临时答案，不执行物理删除。
     */
    @Transactional
    @Override
    public void abandon(Long sessionId) {
        // sessionId 不能为空。
        if (sessionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 只能放弃属于当前用户自己的考试。
        CertificateExamSession session = getOwnedSession(sessionId);

        // 非进行中状态已经是终态，重复放弃时直接返回即可。
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            return;
        }

        // 如果用户点击放弃时已经超时，应当按超时交卷，而不是按主动放弃处理。
        if (isExpired(session)) {
            finishSession(session, ExamSessionStatus.EXPIRED);
            return;
        }

        // 将状态改为 ABANDONED，startOrResume 下次会创建一个全新的随机场次。
        session.setStatus(ExamSessionStatus.ABANDONED);

        // 保存放弃状态。
        certificateExamSessionMapper.updateById(session);
    }

    /**
     * 查询题库当前有效的认证题；这个方法只在创建新考试时调用。
     */
    private List<CertificateQuestionInfo> findReleasedQuestions(Long bankId) {
        // 先查询题库与题目的关联记录。
        List<QuestionBankQuestion> bankQuestions = questionBankQuestionMapper.selectList(
                new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getBankId, bankId)
        );

        if (bankQuestions.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> questionIds = bankQuestions.stream()
                .map(QuestionBankQuestion::getQuestionId)
                .toList();

        List<CertificateQuestionInfo> questionInfos = certificateQuestionInfoMapper.selectList(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .in(CertificateQuestionInfo::getId, questionIds)
                        .eq(CertificateQuestionInfo::getIsReleased, true)
                        .in(
                                CertificateQuestionInfo::getQuestionType,
                                QuestionInfoQuestionType.SINGLE_CHOICE,
                                QuestionInfoQuestionType.MULTIPLE
                        )
        );
        if (questionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        List<Long> validQuestionIds = questionInfos.stream().map(CertificateQuestionInfo::getId).toList();

        // 返回可修改列表，因为 startOrResume 随后需要调用 Collections.shuffle。
        return new ArrayList<>(questionInfos);
    }

    /**
     * 查询考试，同时验证它属于当前登录用户。
     */
    private CertificateExamSession getOwnedSession(Long sessionId) {
        // 从登录上下文取得当前用户。
        Long userId = LoginUserHolder.getUserId();

        // id 和 userId 必须同时匹配，避免越权读取或修改其他用户的考试。
        CertificateExamSession session = certificateExamSessionMapper.selectOne(
                new LambdaQueryWrapper<CertificateExamSession>()
                        .eq(CertificateExamSession::getId, sessionId)
                        .eq(CertificateExamSession::getUserId, userId)
        );

        // 查不到时对外统一表现为参数或数据无效，不泄露其他用户是否存在该场次。
        if (session == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 返回已经完成归属校验的考试实体。
        return session;
    }

    /**
     * 判断考试是否已经到达截止时间。
     */
    private boolean isExpired(CertificateExamSession session) {
        // 没有截止时间属于数据库异常，不能让考试无限进行。
        if (session.getExpiresAt() == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 当前时间等于或晚于 expiresAt 时，都算作考试过期（也就是isExpired == true）
        return !LocalDateTime.now().isBefore(session.getExpiresAt());
    }

    /**
     * 确认当前场次仍允许修改答案。
     */
    private void ensureInProgress(CertificateExamSession session) {
        // 只有 IN_PROGRESS 状态能保存答案。
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new HomeworkException(ResultCodeEnum.REPEAT_SUBMIT);
        }
    }

    /**
     * 检查用户选择是否合法。
     */
    private void validateChosenOptions(CertificateQuestionInfo questionInfo, List<String> chosenOptions) {
        // 题目没有选项属于题库数据错误。
        if (questionInfo.getOptions() == null || questionInfo.getOptions().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 空列表表示清空当前选择，属于合法操作，不需要继续校验。
        if (chosenOptions.isEmpty()) {
            return;
        }

        // 转成 Set 后数量减少，说明前端重复提交了同一个选项。
        Set<String> chosenOptionSet = new HashSet<>(chosenOptions);
        if (chosenOptionSet.size() != chosenOptions.size()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 用户提交的所有选项都必须存在于题目的 options 中。
        if (!new HashSet<>(questionInfo.getOptions()).containsAll(chosenOptionSet)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 单选题最多只能选择一个选项。
        if (questionInfo.getQuestionType() == QuestionInfoQuestionType.SINGLE_CHOICE
                && chosenOptions.size() != 1) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    //CertificateExamVO = CertificateExamSession + CertificateExamAnswer + CertificationQuestionInfo
    //这个方法的作用：把创建好的session，和题目信息 + 考试已经作答信息（如有） 一起返回给前端，这样浏览器在点击“考试模式”后，才能显示具体的考试题目
    private CertificateExamVO buildExamVO(CertificateExamSession session) {
        if(session == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if (session.getQuestionOrder() == null || session.getQuestionOrder().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        List<Long> questionOrder = session.getQuestionOrder();

        // 这里不再过滤 isReleased，因为考试开始后取消发布也不应破坏正在进行的场次。
        //其实这一步queryWrapper，主要是为了查出来 题目列表，然后最终目的是制作成 questionMap
        //这个 questionOrder(verifiedQuestionIds) 都是筛选过的，而且只能是这些questionIds
        LambdaQueryWrapper<CertificateQuestionInfo> questionQueryWrapper = new LambdaQueryWrapper<>();
        questionQueryWrapper.in(CertificateQuestionInfo::getId, questionOrder)
                        .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);

        List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(questionQueryWrapper);

        // 一次性查询全部题目，再使用 Map 按 questionId 快速查找。
        Map<Long, CertificateQuestionInfo> questionMap = certificateQuestionInfos.stream().collect(Collectors.toMap(CertificateQuestionInfo::getId, question -> question));

        // 查看用户在这个session内，答了多少题，允许answers列表为空，那就是一道题也没做
        List<CertificateExamAnswer> answers = certificateExamAnswerMapper.selectList(
                new LambdaQueryWrapper<CertificateExamAnswer>()
                        .eq(CertificateExamAnswer::getSessionId, session.getId())
                        .eq(CertificateExamAnswer::getUserId, session.getUserId())
                        .in(CertificateExamAnswer::getQuestionId, questionOrder)
        );

        // answerMap 让循环中不需要每道题再查询一次数据库。
        Map<Long, CertificateExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(CertificateExamAnswer::getQuestionId, answer -> answer));

        //开始组装
        CertificateExamVO examVO = new CertificateExamVO();
        //第一步：组装题目列表，如有已做的题，也要把用户选项一起返回
        List<CertificateExamQuestionVO> questionVOs = new ArrayList<>();

        // 按照 questionOrder的Id顺序遍历，才能保证同一session内的题目顺序一致
        questionOrder.forEach(questionId -> {
            CertificateExamQuestionVO vo = new CertificateExamQuestionVO();
            CertificateQuestionInfo questionInfo = questionMap.get(questionId);
            if(questionInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            vo.setQuestionId(questionInfo.getId());
            vo.setTitle(questionInfo.getTitle());
            vo.setOptions(questionInfo.getOptions());
            vo.setQuestionType(questionInfo.getQuestionType());
            vo.setImageUrl(questionInfo.getImageUrl());

            CertificateExamAnswer answer = answerMap.get(questionId); //这一步就是看用户答没答
            if(answer != null) { //答了，就把用户选择的选项返回
                vo.setChosenOptions(answer.getChosenOptions());
                if(answer.getChosenOptions() != null && !answer.getChosenOptions().isEmpty()) {
                    vo.setAnswered(true);
                }
            }
            questionVOs.add(vo);
        });
        examVO.setQuestions(questionVOs);
        examVO.setSessionId(session.getId());
        examVO.setBankId(session.getBankId());
        examVO.setExpiresAt(session.getExpiresAt());
        examVO.setStatus(session.getStatus());

        return examVO;
    }

    /**
     * 统一判题并保存考试最终状态。
     */
    private BankFinishVO finishSession(CertificateExamSession session, ExamSessionStatus finalStatus) {
        // 先根据本场题目和临时答案生成结算结果。
        BankFinishVO finishVO = buildFinishVO(session);

        // 从结算结果中取得统计信息。
        QuestionCountVO questionCount = finishVO.getQuestionCount();

        // 保存最终答对数量，方便以后查询历史成绩时直接读取。
        session.setCorrectCount(questionCount.getCorrectCount());

        // 保存最终正确率。
        session.setCorrectRate(questionCount.getCorrectRate());

        // 保存实际结束时间；超时场次也记录系统结算时间。
        session.setSubmittedAt(LocalDateTime.now());

        // 状态可能是用户主动提交，也可能是系统判定超时。
        session.setStatus(finalStatus);

        // 更新考试场次记录。
        certificateExamSessionMapper.updateById(session);

        // 返回包含答案解析和统计数据的结算页面。
        return finishVO;
    }

    /**
     * 根据临时答案构造提交后的完整结算结果。
     */
    private BankFinishVO buildFinishVO(CertificateExamSession session) {
        // 取得本场考试的固定题目顺序。
        List<Long> questionOrder = requireQuestionOrder(session);

        // 查询所有题目并转成 Map，避免循环查询数据库。
        Map<Long, CertificateQuestionInfo> questionMap = findSessionQuestions(questionOrder).stream()
                .collect(Collectors.toMap(CertificateQuestionInfo::getId, question -> question));

        // 查询本场考试的全部临时答案。
        List<CertificateExamAnswer> answers = certificateExamAnswerMapper.selectList(
                new LambdaQueryWrapper<CertificateExamAnswer>()
                        .eq(CertificateExamAnswer::getSessionId, session.getId())
                        .eq(CertificateExamAnswer::getUserId, session.getUserId())
        );

        // 转成 questionId 到答案的 Map，方便 O(1) 查找。
        Map<Long, CertificateExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(CertificateExamAnswer::getQuestionId, answer -> answer));

        // 用于保存提交后每一道题的题目、用户选择、正确答案和解析。
        List<CertificateQuestionReviewVO> reviewVOs = new ArrayList<>();

        // 记录用户实际作答数量。
        long answeredCount = 0L;

        // 记录答对数量。
        long correctCount = 0L;

        // 按考试时的随机顺序组装结算页面。
        for (Long questionId : questionOrder) {
            // 取得题目详情。
            CertificateQuestionInfo questionInfo = questionMap.get(questionId);

            // 缺少题目时不能继续判题。
            if (questionInfo == null
                    || questionInfo.getCorrectAnswer() == null
                    || questionInfo.getCorrectAnswer().isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }

            // 取得用户对这道题保存的最后选择。
            CertificateExamAnswer answer = answerMap.get(questionId);

            // 只有选择列表非空时才算已经作答。
            boolean answered = answer != null
                    && answer.getChosenOptions() != null
                    && !answer.getChosenOptions().isEmpty();

            // 未作答题默认不算正确；已作答题比较选项集合。
            boolean correct = answered
                    && sameOptions(answer.getChosenOptions(), questionInfo.getCorrectAnswer());

            // 已作答时累计 answeredCount。
            if (answered) {
                answeredCount++;
            }

            // 答案正确时累计 correctCount。
            if (correct) {
                correctCount++;
            }

            // 创建提交后单道题的 ReviewVO。
            CertificateQuestionReviewVO reviewVO = new CertificateQuestionReviewVO();

            // 设置题目基本信息。
            reviewVO.setQuestionId(questionInfo.getId());
            reviewVO.setTitle(questionInfo.getTitle());
            reviewVO.setOptions(questionInfo.getOptions());
            reviewVO.setQuestionType(questionInfo.getQuestionType());
            reviewVO.setImageUrl(questionInfo.getImageUrl());

            // 正式交卷后才返回正确答案与解析。
            reviewVO.setCorrectAnswer(questionInfo.getCorrectAnswer());
            reviewVO.setAnalysis(questionInfo.getAnalysis());

            // 未作答时 chosenOptions 保持 null。
            reviewVO.setChosonOptions(answer == null ? null : answer.getChosenOptions());

            // 未作答时 isCorrect 保持 null；已作答时返回真实判题结果。
            reviewVO.setIsCorrect(answered ? correct : null);

            // 加入整套题的结算列表。
            reviewVOs.add(reviewVO);
        }

        // 本场考试题目总数就是固定题序的长度。
        long totalCount = questionOrder.size();

        // 正确率按“答对题数 / 整套题总数”计算，未答题相当于错误。
        BigDecimal correctRate = totalCount == 0L
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount)
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);

        // 组装统计对象。
        QuestionCountVO questionCountVO = new QuestionCountVO();
        questionCountVO.setTotalCount(totalCount);
        questionCountVO.setAnsweredCount(answeredCount);
        questionCountVO.setCorrectCount(correctCount);
        questionCountVO.setCorrectRate(correctRate);

        // 组装最终结算对象。
        BankFinishVO finishVO = new BankFinishVO();
        finishVO.setCertificateQuestionReviewVos(reviewVOs);
        finishVO.setQuestionCount(questionCountVO);

        // 返回给提交接口。
        return finishVO;
    }

    /**
     * 读取并校验考试场次中的固定题序。
     */
    private List<Long> requireQuestionOrder(CertificateExamSession session) {
        // 空题序意味着考试创建不完整，无法恢复或判题。

    }

    /**
     * 查询某场考试题序中的全部题目。
     */
    private List<CertificateQuestionInfo> findSessionQuestions(List<Long> questionOrder) {
        // 这里不再过滤 isReleased，因为考试开始后取消发布也不应破坏正在进行的场次。
        return certificateQuestionInfoMapper.selectList(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .in(CertificateQuestionInfo::getId, questionOrder)
                        .in(
                                CertificateQuestionInfo::getQuestionType,
                                QuestionInfoQuestionType.SINGLE_CHOICE,
                                QuestionInfoQuestionType.MULTIPLE
                        )
        );
    }

    /**
     * 判断题型是否属于当前支持的认证题类型。
     */
    private boolean isCertificateQuestionType(QuestionInfoQuestionType questionType) {
        // 当前考试模式只支持单选题和多选题。
        return questionType == QuestionInfoQuestionType.SINGLE_CHOICE
                || questionType == QuestionInfoQuestionType.MULTIPLE;
    }

    /**
     * 忽略选项顺序比较用户答案和正确答案。
     */
    private boolean sameOptions(List<String> userOptions, List<String> correctOptions) {
        // 任意一边为空引用都不能判定为正确。
        if (userOptions == null || correctOptions == null) {
            return false;
        }

        // 数量必须相等，并且转换成 Set 后包含完全相同的选项。
        return userOptions.size() == correctOptions.size()
                && new HashSet<>(userOptions).equals(new HashSet<>(correctOptions));
    }
}
