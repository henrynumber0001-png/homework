package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.ExamExpiredException;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.model.entity.CertificateExamAnswer;
import com.homework.model.entity.CertificateExamSession;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.entity.UserFavoriteQuestion;
import com.homework.model.enums.ExamSessionStatus;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.CertificateExamAnswerDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.CertificateExamService;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.PublishedQuestionBankAccessService;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CertificateExamServiceImpl implements CertificateExamService {

    // 当前先把每场考试固定为 60 分钟；以后可以改成从题库配置表读取。
    private static final long EXAM_DURATION_MINUTES = 60L;

    private final CertificateExamSessionMapper certificateExamSessionMapper;

    private final CertificateExamAnswerMapper certificateExamAnswerMapper;

    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;

    private final QuestionBankQuestionMapper questionBankQuestionMapper;

    private final CertificateExamLockMapper certificateExamLockMapper;

    private final UserFavoriteQuestionMapper userFavoriteQuestionMapper;

    private final MembershipAccessService membershipAccessService;
    private final PublishedQuestionBankAccessService publishedQuestionBankAccessService;
    private final CosReadUrlSigner readUrlSigner;


    @Transactional(noRollbackFor = ExamExpiredException.class)
    @Override
    //前端只有 bankId，还不知道 sessionId 时调用，例如用户从题库列表点击“开始考试”。
    //它更像“进入考试的总入口”。
    public CertificateExamVO startOrResume(Long bankId) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());

        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long userId = LoginUserHolder.getUserId();

        // 查询该用户在这个题库中【最近一场】仍标记为“进行中”的考试。
        // 第一步：保证锁记录存在。
        certificateExamLockMapper.ensureLockRow(userId, bankId); //没有锁（行数据），就创建一条；有则忽略，什么都不做，直接放行；

        // 第二步：锁住当前“用户 + 题库”。
        Long lockId = certificateExamLockMapper.lockUserBank(userId, bankId);
        if (lockId == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 第三步：获得用户题库级锁后，再查询进行中的考试。
        CertificateExamSession activeSession =
                certificateExamSessionMapper.selectOne(
                        new LambdaQueryWrapper<CertificateExamSession>()
                                .eq(CertificateExamSession::getUserId, userId)
                                .eq(CertificateExamSession::getBankId, bankId)
                                .eq(CertificateExamSession::getStatus, ExamSessionStatus.IN_PROGRESS)
                                .orderByDesc(CertificateExamSession::getId)
                                .last("LIMIT 1 FOR UPDATE")
                );

        // 如果 CertificateExamSession 中的最新一条 activeSession 不为空，说明有进行中的考试。
        // 看看最新的这条 activeSession 是否已过期（isExpired == true）
        if (activeSession != null) {
            // 如果还没过期，返回原 session，继续答题。
            // 因此，刷新页面后题目顺序和用户选择不会改变。
            if (!isExpired(activeSession)) {
                return buildExamVO(activeSession);
            }

            //如果已经过期，结算后继续向下执行创建新场次的逻辑
            //把这条session的状态调整到 EXPIRED，并更新 certificate_exam_session
            finishSession(activeSession, ExamSessionStatus.EXPIRED);
        }

        //如果没有 activeSession，那么就开始创建新的session
        publishedQuestionBankAccessService.requirePublished(bankId);
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

        List<CertificateQuestionInfo> certQuestionInfos = certificateQuestionInfoMapper.selectList(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .in(CertificateQuestionInfo::getId, questionIds)
                        .eq(CertificateQuestionInfo::getIsReleased, true)
                        .in(
                                CertificateQuestionInfo::getQuestionType,
                                QuestionInfoQuestionType.SINGLE_CHOICE,
                                QuestionInfoQuestionType.MULTIPLE
                        )
        );
        if (certQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 返回可修改列表，因为 startOrResume 随后需要调用 Collections.shuffle。
        ArrayList<CertificateQuestionInfo> questionInfos = new ArrayList<>(certQuestionInfos);

        // 只在创建新场次时随机一次；随机后的结果马上保存到 questionOrder，今后只要进入这个 session，题目就按照这个 questionOrder 排列。
        Collections.shuffle(questionInfos);

        // questionOrder = verifiedQuestionIds
        List<Long> questionOrder = questionInfos.stream()
                .map(CertificateQuestionInfo::getId)
                .toList();

        // 获取一次当前时间，避免 startedAt 和 expiresAt 因两次取值产生细小偏差。
        LocalDateTime now = LocalDateTime.now();

        //没有session，就创建一个session
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


        certificateQuestionInfoMapper.certificateViewCount(bankId);
        return buildExamVO(newSession);//这个时候还没返回题目列表给前端呢，必须要调用buildExamVO之后
    }


    @Transactional
    @Override
    //前端已经知道 sessionId，希望精确恢复这一场考试时调用（浏览器刷新页面/从考试页面重新加载）
    public CertificateExamVO getSession(Long sessionId) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (sessionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long userId = LoginUserHolder.getUserId();

        CertificateExamSession session = getOwnedSessionForUpdate(sessionId, userId);

        // 能查到，接下来看看这个session的状态是否过期
        if (session.getStatus() == ExamSessionStatus.IN_PROGRESS && isExpired(session)) {
            // 超时后统一判题，并把状态改为 EXPIRED。
            finishSession(session, ExamSessionStatus.EXPIRED);
        }
        // 没过期，查数据，返回给前端
        return buildExamVO(session);
    }


    @Transactional(noRollbackFor = ExamExpiredException.class) //确保自动交卷不会回滚
    @Override
    public void saveAnswer(CertificateExamAnswerDTO dto) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        // buildExamVO() 是“后端向前端返回数据”，而 saveAnswer() 是“后端接收前端传回的数据”。
        // 后端不能因为之前返回过正确数据，就默认之后收到的数据一定没被修改。

        if (dto == null || dto.getChosenOptions() == null || dto.getSessionId() == null || dto.getQuestionId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long userId = LoginUserHolder.getUserId();

        // 锁定 session，避免保存答案和提交考试并发执行。
        CertificateExamSession session = getOwnedSessionForUpdate(
                dto.getSessionId(),
                userId
        );

        // 能查到，接下来看看这个session的状态是否过期
        if (session.getStatus() == ExamSessionStatus.IN_PROGRESS && isExpired(session)) {
            // 超时后统一判题，并把状态改为 EXPIRED。
            finishSession(session, ExamSessionStatus.EXPIRED);
            throw new ExamExpiredException();
        }

        //读取 session，发现已经是 SUBMITTED 或 EXPIRED，没有修改任何数据库数据
        //这里没有需要保留的数据库修改，因此不应该使用特殊的“不回滚异常”，还是要回滚的，因为未执行成功。
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new HomeworkException(ResultCodeEnum.REPEAT_SUBMIT);
        }

        // questionOrder 是本场考试允许作答的题目白名单。
        if (session.getQuestionOrder() == null || !session.getQuestionOrder().contains(dto.getQuestionId())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 从数据库读取真实题目，不能相信前端自己声明题型或选项范围。
        CertificateQuestionInfo questionInfo = certificateQuestionInfoMapper.selectById(dto.getQuestionId());

        // 题目被删除或题型不属于认证题时，当前考试数据已经不完整。
        if (questionInfo == null || !isCertificateQuestionType(questionInfo.getQuestionType())) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 检查用户提交的每一个选项是否真实存在，并检查单选题数量规则。
        validateChosenOptions(questionInfo, dto.getChosenOptions());

        // 同一场考试的同一道题只允许有一条临时答案，因此先查询旧记录。
        CertificateExamAnswer savedAnswer = certificateExamAnswerMapper.selectOne(
                new LambdaQueryWrapper<CertificateExamAnswer>()
                        .eq(CertificateExamAnswer::getSessionId, dto.getSessionId())
                        .eq(CertificateExamAnswer::getQuestionId, dto.getQuestionId())
        );

        ArrayList<String> chosenOptionsList = new ArrayList<>(dto.getChosenOptions());
        // 如果没旧记录，插入一条新记录。
        if (savedAnswer == null) {
            CertificateExamAnswer newAnswer = new CertificateExamAnswer();
            newAnswer.setSessionId(dto.getSessionId());
            newAnswer.setUserId(session.getUserId());
            newAnswer.setQuestionId(dto.getQuestionId());
            newAnswer.setChosenOptions(chosenOptionsList);
            newAnswer.setAnsweredAt(LocalDateTime.now());
            certificateExamAnswerMapper.insert(newAnswer);
            return;
        }

        // 如果有旧记录，直接覆盖选择，保证每场考试每道题只有一个最新状态。
        savedAnswer.setChosenOptions(chosenOptionsList);
        savedAnswer.setAnsweredAt(LocalDateTime.now());
        certificateExamAnswerMapper.updateById(savedAnswer);
    }

    @Transactional
    @Override
    public BankFinishVO submit(Long sessionId) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (sessionId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //先检查session是否存在
        Long userId = LoginUserHolder.getUserId();

        // 与 saveAnswer 锁定同一行。
        CertificateExamSession session = getOwnedSessionForUpdate(sessionId, userId);

        //这个条件判断用于解决：
        // 用户重复点击提交的问题，比如双击“提交试卷”，可能发出两个请求
        // 第一次提交实际上已经成功，但后端响应在网络中断了。前端没有收到结果，可能重新发送一次。
        // 前端倒计时与后端时间不同步：即使页面上按钮还能点击，也不代表服务器认为考试没有结束（前端慢了，实际上服务器端已经自动提交了）。
        if (session.getStatus() == ExamSessionStatus.SUBMITTED || session.getStatus() == ExamSessionStatus.EXPIRED) {
            return buildFinishVO(session);
        }

        // 用户在截止时间之后被动交卷，归类为expired。
        ExamSessionStatus finalStatus = isExpired(session) ? ExamSessionStatus.EXPIRED : ExamSessionStatus.SUBMITTED;

        // 统一判题、保存统计结果并返回答案解析。
        return finishSession(session, finalStatus);
    }

    private boolean isExpired(CertificateExamSession session) {
        // 没有截止时间属于数据库异常，不能让考试无限进行。
        if (session.getExpiresAt() == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 当前时间等于或晚于 expiresAt 时，都算作考试过期（也就是isExpired == true）
        return !LocalDateTime.now().isBefore(session.getExpiresAt());
    }

    //检查用户选择是否合法
    private void validateChosenOptions(CertificateQuestionInfo questionInfo, List<String> chosenOptions) {
        // 题目没有选项属于题库数据错误。
        if (questionInfo.getOptions() == null || questionInfo.getOptions().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        // 空列表表示清空当前选择，属于合法操作，不需要继续校验下面的内容了。
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
        if (session == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if (session.getQuestionOrder() == null || session.getQuestionOrder().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        List<Long> questionOrder = session.getQuestionOrder();
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(
                session.getUserId(), session.getBankId(), questionOrder);

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
            if (questionInfo == null) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            vo.setQuestionId(questionInfo.getId());
            vo.setTitle(questionInfo.getTitle());
            vo.setOptions(questionInfo.getOptions());
            vo.setQuestionType(questionInfo.getQuestionType());
            vo.setImageUrl(readUrlSigner.sign(questionInfo.getImageObjectKey()));
            vo.setIsFavorite(favoriteQuestionMap.containsKey(questionId));

            CertificateExamAnswer answer = answerMap.get(questionId); //这一步就是看用户答没答
            if (answer != null) { //答了，就把用户选择的选项返回
                vo.setChosenOptions(answer.getChosenOptions());
                if (answer.getChosenOptions() != null && !answer.getChosenOptions().isEmpty()) {
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

    //用于表示 用户首次点击“提交试卷”之后， 返回结算页面 + 保存正确率和答题时间到当前certificate_exam_session
    private BankFinishVO finishSession(CertificateExamSession session, ExamSessionStatus finalStatus) {
        // 先根据本场题目和临时答案生成结算结果。
        BankFinishVO finishVO = buildFinishVO(session);

        // 从结算结果中取得统计信息。
        QuestionCountVO questionCount = finishVO.getQuestionCount();

        session.setCorrectCount(questionCount.getCorrectCount());
        session.setCorrectRate(questionCount.getCorrectRate());
        session.setSubmittedAt(LocalDateTime.now());
        session.setStatus(finalStatus);

        certificateExamSessionMapper.updateById(session);

        return finishVO;
    }

    // 这个方法单纯的返回 结算页面 给前端，用于处理重复提交问题
    private BankFinishVO buildFinishVO(CertificateExamSession session) {
        if (session == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if (session.getQuestionOrder() == null || session.getQuestionOrder().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        List<Long> questionOrder = session.getQuestionOrder();
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(
                session.getUserId(), session.getBankId(), questionOrder);

        //根据 questionOrder(verifiedQuestionIds) 再去CertificateQuestionInfo 查询一遍 题目列表
        //普通下架情况下，结算时继续显示并判定这道题是合理的，因此只在创建session的时候，拿到questionOrder之前 执行 isReleased 过滤
        LambdaQueryWrapper<CertificateQuestionInfo> questionQueryWrapper = new LambdaQueryWrapper<>();
        questionQueryWrapper.in(CertificateQuestionInfo::getId, questionOrder)
                .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);

        List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(questionQueryWrapper);
        if (certificateQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        Map<Long, CertificateQuestionInfo> questionMap = certificateQuestionInfos.stream()
                .collect(Collectors.toMap(CertificateQuestionInfo::getId, question -> question));

        // 查询该 userId 在这个 session 之下，答了多少题
        LambdaQueryWrapper<CertificateExamAnswer> answerQueryWrapper = new LambdaQueryWrapper<>();
        answerQueryWrapper.eq(CertificateExamAnswer::getSessionId, session.getId())
                .eq(CertificateExamAnswer::getUserId, session.getUserId());
        List<CertificateExamAnswer> answers = certificateExamAnswerMapper.selectList(answerQueryWrapper);
//        long answeredCount = certificateExamAnswerMapper.selectCount(answerQueryWrapper);

        // 转成 questionId 到答案的 Map，方便 O(1) 查找。
        Map<Long, CertificateExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(CertificateExamAnswer::getQuestionId, answer -> answer));

        // 用于保存提交后每一道题的题目、用户选择、正确答案和解析。
        List<CertificateQuestionReviewVO> reviewVOs = new ArrayList<>();

        // 记录用户实际作答数量。
        long answeredCount = 0L;

        // 记录答对数量。
        long correctCount = 0L;

        for (Long questionId : questionOrder) {
            CertificateQuestionInfo questionInfo = questionMap.get(questionId);

            // 题目缺少“正确答案”时，抛异常
            if (questionInfo == null || questionInfo.getCorrectAnswer() == null || questionInfo.getCorrectAnswer().isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }

            // 取得用户对这道题保存的最后选择。
            CertificateExamAnswer answer = answerMap.get(questionId);

            // 只有选择列表非空时才算已经作答。
            boolean answered =
                    answer != null && answer.getChosenOptions() != null && !answer.getChosenOptions().isEmpty();

            // 未作答题默认不算正确；已作答题比较选项集合。
            boolean correct = answered && sameOptions(answer.getChosenOptions(), questionInfo.getCorrectAnswer());

            // 已作答时累计 answeredCount（因为空选项也会被存入CertificateExamAnswer,所以不能直接selectCount）
            if (answered) {
                answeredCount++;
            }

            // 答案正确时累计 correctCount。
            if (correct) {
                correctCount++;
            }

            CertificateQuestionReviewVO reviewVO = new CertificateQuestionReviewVO();
            reviewVO.setQuestionId(questionInfo.getId());
            reviewVO.setTitle(questionInfo.getTitle());
            reviewVO.setOptions(questionInfo.getOptions());
            reviewVO.setQuestionType(questionInfo.getQuestionType());
            reviewVO.setImageUrl(readUrlSigner.sign(questionInfo.getImageObjectKey()));
            reviewVO.setCorrectAnswer(questionInfo.getCorrectAnswer());
            reviewVO.setAnalysis(questionInfo.getAnalysis());
            reviewVO.setIsFavorite(favoriteQuestionMap.containsKey(questionId));

            reviewVO.setChosenOptions(answer == null ? null : answer.getChosenOptions());
            reviewVO.setIsCorrect(answered ? correct : null); //answered 本身结果就是 true/false

            reviewVOs.add(reviewVO);
        }

        long totalCount = questionOrder.size();
        BigDecimal correctRate;
        if (totalCount == 0) {
            correctRate = BigDecimal.ZERO;
        } else {
            correctRate = BigDecimal.valueOf(correctCount)
                    .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
        }

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
     * 判断题型是否属于当前支持的认证题类型。
     */
    private boolean isCertificateQuestionType(QuestionInfoQuestionType questionType) {
        // 当前考试模式只支持单选题和多选题。
        return questionType == QuestionInfoQuestionType.SINGLE_CHOICE
                || questionType == QuestionInfoQuestionType.MULTIPLE;
    }

    private Map<Long, UserFavoriteQuestion> getFavoriteQuestionMap(Long userId, Long bankId,
                                                                   Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return userFavoriteQuestionMapper.selectList(
                        new LambdaQueryWrapper<UserFavoriteQuestion>()
                                .eq(UserFavoriteQuestion::getUserId, userId)
                                .eq(UserFavoriteQuestion::getBankId, bankId)
                                .in(UserFavoriteQuestion::getQuestionId, questionIds)
                ).stream()
                .collect(Collectors.toMap(UserFavoriteQuestion::getQuestionId, favorite -> favorite));
    }

    /**
     * 忽略选项顺序比较用户答案和正确答案。
     */
    private boolean sameOptions(List<String> userOptions, List<String> correctOptions) {
        // 为什么 userOptions == null 不报错？ 因为这是一个 boolean 判断对错的方法，不应该赋予它过重的职责
        if (userOptions == null || correctOptions == null) {
            return false;
        }

        // 数量必须相等，并且转换成 Set 后包含完全相同的选项。
        return userOptions.size() == correctOptions.size()
                && new HashSet<>(userOptions).equals(new HashSet<>(correctOptions));
        //第二步把 List 转成 Set：原因是多选题不应该关心选项顺序。
    }

    private CertificateExamSession getOwnedSessionForUpdate(Long sessionId, Long userId) {
        CertificateExamSession session = certificateExamSessionMapper.selectOne(
                new LambdaQueryWrapper<CertificateExamSession>()
                        .eq(CertificateExamSession::getId, sessionId)
                        .eq(CertificateExamSession::getUserId, userId)
                        .last("FOR UPDATE") //把对应的 certificate_exam_session 行锁定，直到当前事务提交或回滚。
        );

        if (session == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        return session;
    }
}
