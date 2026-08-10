package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.*;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.AiEvaluationService;
import com.homework.web.app.service.LlmClient;
import com.homework.web.app.service.LlmResponse;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.service.QuestionInfoService;
import com.homework.web.app.service.PublishedQuestionBankAccessService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class QuestionInfoServiceImpl implements QuestionInfoService {
    private static final String RULE_WELCOME_MODEL = "rule-welcome";

    private final InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final AiEvaluationService aiEvaluationService;
    private final QuestionAiEvaluationMapper questionAiEvaluationMapper;
    private final UserQuestionNoteMapper userQuestionNoteMapper;
    private final CertificateQuestionInfoMapper certificateQuestionInfoMapper;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final LlmClient llmClient;
    private final AiPromptBuilder aiPromptBuilder;
    private final UserFavoriteQuestionMapper userFavoriteQuestionMapper;
    private final MembershipAccessService membershipAccessService;
    private final UserBankCorrectRateMapper userBankCorrectRateMapper;
    private final PublishedQuestionBankAccessService publishedQuestionBankAccessService;
    private final CosReadUrlSigner readUrlSigner;

    @Override
    public List<InterviewQuestionPageVO> getInterviewByBankId(Long bankId) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());

        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 变更：原来先查关系表再用 IN 查询；现在直接按面试题 bank_id 查询并按手动顺序返回。
        LambdaQueryWrapper<InterviewQuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
        questionInfoQueryWrapper.eq(InterviewQuestionInfo::getBankId, bankId)
                .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY)
                .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .orderByAsc(InterviewQuestionInfo::getQuestionNo)
                .orderByAsc(InterviewQuestionInfo::getId);

        List<InterviewQuestionInfo> questionInfos = interviewQuestionInfoMapper.selectList(questionInfoQueryWrapper);
        if (questionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        List<Long> questionIds = questionInfos.stream().map(InterviewQuestionInfo::getId).toList();

        // 以 questionId 为键，既能 O(1) 判断收藏状态，也保留完整收藏记录供后续使用。
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, questionIds);

        List<InterviewQuestionPageVO> list = new ArrayList<>();
        questionInfos.forEach(questionInfo -> {
            InterviewQuestionPageVO vo = new InterviewQuestionPageVO();

            vo.setQuestionId(questionInfo.getId());
            vo.setTitle(questionInfo.getTitle());

            //interview_question_info里存的是正式的 objectKey
            //因为存储桶的权限模式是 私有读写，因此所有的 读取图片 的操作，都必须调用 readUrlSigner.sign 先进行鉴权（设置算法 + 签发AKID + 签名有效期（根据ttl）+ SecretKey生效时间 + 签名本身）
            //获得 presignUrl 之后，返回给前端
            //前端拿着这个 presignUrl 再次请求COS服务器的存储桶，最终获得二进制图片，给到浏览器（这一步不经过后端了）
            //不必返回给前端 imageObjectKey, 因为 objectKey 已经存储到 存储桶和数据库表了（各保存一份），后端不需要再携带 objectKey 返回给前端用于bind到数据库题目表了
            vo.setImageUrl(readUrlSigner.sign(questionInfo.getImageObjectKey()));
            vo.setQuestionType(questionInfo.getQuestionType());
            vo.setIsFavorite(favoriteQuestionMap.containsKey(questionInfo.getId())); //containsKey 比 favoriteQuestionMap.get(questionInfo.getId()) != null 更优雅
            list.add(vo);
        });

        interviewQuestionInfoMapper.incrementViewCount(bankId);
        return list;
    }

    @Transactional
    @Override
    public InterViewAnswerPageVO getInterviewAnswer(InterviewQuestionSubmitDTO submitDTO) {

        //调用 membershipAccessService 获取当前登录用户的会员信息
        MembershipAccessSnapshot membershipAccessSnapshot = membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());

        //允许用户输入的回答为空
        if (submitDTO == null || submitDTO.getQuestionId() == null || submitDTO.getBankId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long questionId = submitDTO.getQuestionId();
        Long bankId = submitDTO.getBankId();
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 变更：关系表已删除，bankId + questionId 归属校验直接合并到题目实体查询中。
        LambdaQueryWrapper<InterviewQuestionInfo> interviewQueryWrapper = new LambdaQueryWrapper<>();
        interviewQueryWrapper.eq(InterviewQuestionInfo::getId, questionId)
                .eq(InterviewQuestionInfo::getBankId, bankId)
                .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY)
                .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED);

        InterviewQuestionInfo questionInfo = interviewQuestionInfoMapper.selectOne(interviewQueryWrapper);

        List<Long> questionIdList = List.of(questionId); //把单个对象转换成List集合
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId,questionIdList);

        if (questionInfo == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //兜底机制（但后端接口不能只相信前端，因为用户可以绕过页面，直接请求）
        if (!questionInfo.getQuestionType().equals(QuestionInfoQuestionType.ESSAY)
                || questionInfo.getStatus() != QuestionInfoStatus.PUBLISHED) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String title = questionInfo.getTitle();
        String analysis = questionInfo.getAnalysis();
        String content = submitDTO.getContent();

        // Premium Plus 才返回 AI 评价；Premium 仍可查看题目解析。
        boolean premiumPlus = membershipAccessSnapshot.status() == MembershipStatus.PREMIUM_PLUS;
        AiEvaluationResult aiResult = premiumPlus ? aiEvaluationService.evaluateInterviewAnswer(title, content, analysis) : null;

        //返回给前端
        InterViewAnswerPageVO answer = new InterViewAnswerPageVO();
        answer.setQuestionId(questionId);
        answer.setAnalysis(questionInfo.getAnalysis());
        answer.setAiResult(aiResult);
        answer.setAiEvaluationEnabled(premiumPlus);
        answer.setIsFavorite(favoriteQuestionMap.containsKey(questionId));

        //把用户输入的回答放到 用户ID下的专门的一张表 user_question_answer, 用于用户其他信息查询功能（如答题历史、收藏、错题）
        //后端先保存 UserQuestionAnswer，拿到 answerId
        Long userId = LoginUserHolder.getUserId();
        UserQuestionAnswer userAnswer = new UserQuestionAnswer();
        userAnswer.setUserId(userId);
        userAnswer.setBankId(bankId);
        userAnswer.setQuestionId(questionId);
        userAnswer.setContent(submitDTO.getContent());
        userAnswer.setQuestionType(QuestionInfoQuestionType.ESSAY);
        if (aiResult != null) {
            userAnswer.setAiScoreRate(aiResult.getScoreRate());
            userAnswer.setIsCorrect(aiResult.getScoreRate().compareTo(BigDecimal.valueOf(60)) >= 0);
        }
        userAnswer.setAnsweredTime(LocalDateTime.now());

        Long answerId = saveOrUpdateLatestAnswer(userAnswer);
        //UserQuestionAnswer 负责记录：用户每次提交的答案
        //QuestionAiEvaluation 负责记录：AI 对这个用户这次答案的评价

        //记录一次 AI 对这次用户提交答案的评价
        if (aiResult != null) {
            QuestionAiEvaluation questionAiEvaluation = new QuestionAiEvaluation();
            questionAiEvaluation.setUserId(userId);
            questionAiEvaluation.setQuestionId(questionId);
            questionAiEvaluation.setAnswerId(answerId);
            BeanUtils.copyProperties(aiResult, questionAiEvaluation);
            saveOrUpdateLatestEvaluation(questionAiEvaluation);
        }
        return answer;
    }

    @Transactional
    @Override
    public List<InterviewQuestionReviewVO> getInterviewQuestionReview(Long bankId) {

        //调用 membershipAccessService 获取当前登录用户的会员信息
        MembershipAccessSnapshot membershipAccessSnapshot = membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 回顾页直接按题目实体 bank_id 查询，question_no 是题库内序号来源。
        LambdaQueryWrapper<InterviewQuestionInfo> interviewQueryWrapper = new LambdaQueryWrapper<>();
        interviewQueryWrapper.eq(InterviewQuestionInfo::getBankId, bankId)
                .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY)
                .orderByAsc(InterviewQuestionInfo::getQuestionNo)
                .orderByAsc(InterviewQuestionInfo::getId);

        List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewQueryWrapper);
        if (interviewQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> interviewQuestionIds = interviewQuestionInfos.stream().map(InterviewQuestionInfo::getId).toList();
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, interviewQuestionIds);

        Long userId = LoginUserHolder.getUserId();

        LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userAnswerQueryWrapper.eq(UserQuestionAnswer::getBankId, bankId)
                .eq(UserQuestionAnswer::getUserId, userId)
                .eq(UserQuestionAnswer::getQuestionType, QuestionInfoQuestionType.ESSAY)
                .in(UserQuestionAnswer::getQuestionId, interviewQuestionIds);

        List<UserQuestionAnswer> userQuestionAnswers = userQuestionAnswerMapper.selectList(userAnswerQueryWrapper);
        //允许 UserQuestionAnswer 列表为空，那就是一道题没做就提交答案了


        //设计成Map集合，就不用每一次都QueryWrapper查询了，提高性能
        Map<Long, UserQuestionAnswer> questionAnswerMap = userQuestionAnswers.stream()
                .collect(Collectors.toMap(UserQuestionAnswer::getQuestionId, userQuestionAnswer -> userQuestionAnswer));


        List<QuestionAiEvaluation> questionAiEvaluations;
        if (userQuestionAnswers.isEmpty()) { //那就是一道题也没答
            questionAiEvaluations = List.of(); //AI评价的列表为空，也就是答题卡上所有题的AI评价部分，内容为空
        } else {
            //查询 userAnswerIds 的主要目的就是为了获得 questionAiEvaluations
            List<Long> userAnswerIds = userQuestionAnswers.stream().map(UserQuestionAnswer::getId).collect(Collectors.toList());

            //所以，查询ai_evaluation_result表中中，answer_id 对应 user_question_answer.id的数据
            LambdaQueryWrapper<QuestionAiEvaluation> questionAiEvaluationQueryWrapper = new LambdaQueryWrapper<>();
            questionAiEvaluationQueryWrapper.eq(QuestionAiEvaluation::getUserId, userId)
                    .in(QuestionAiEvaluation::getAnswerId, userAnswerIds);

            questionAiEvaluations = questionAiEvaluationMapper.selectList(questionAiEvaluationQueryWrapper);
        }

        //同样也做成Map集合，避免反复查询（空集合，null也是要返回的）
        //空的 questionAiEvaluations 集合也要返回，因为这个是整个题库提交后的总结页面，AI评价为空，不代表不用返回题目的标题、答案解析、用户回答文本
        Map<Long, QuestionAiEvaluation> questionAiEvaluationMap = questionAiEvaluations.stream()
                .collect(Collectors.toMap(QuestionAiEvaluation::getAnswerId, questionAiEvaluation -> questionAiEvaluation));

        //开始组装 List<InterviewQuestionReviewVO>
        List<InterviewQuestionReviewVO> questionReviewVos = new ArrayList<>();
        interviewQuestionInfos.forEach(questionInfo -> {
            InterviewQuestionReviewVO vo = new InterviewQuestionReviewVO();
            vo.setQuestionId(questionInfo.getId());
            vo.setTitle(questionInfo.getTitle());
            vo.setImageUrl(readUrlSigner.sign(questionInfo.getImageObjectKey()));
            vo.setAnalysis(questionInfo.getAnalysis());
            vo.setQuestionType(questionInfo.getQuestionType());
            vo.setIsFavorite(favoriteQuestionMap.containsKey(questionInfo.getId()));
            UserQuestionAnswer userQuestionAnswer = questionAnswerMap.get(questionInfo.getId());
            if (userQuestionAnswer != null) {
                vo.setContent(userQuestionAnswer.getContent());
                QuestionAiEvaluation questionAiEvaluation = questionAiEvaluationMap.get(userQuestionAnswer.getId());
                if (questionAiEvaluation != null) {
                    AiEvaluationResult aiResult = fetchAiResult(questionAiEvaluation);
                    vo.setAiResult(aiResult);
                    vo.setIsCorrect(aiResult.getScoreRate().compareTo(BigDecimal.valueOf(60)) >= 0);
                }
            }
            questionReviewVos.add(vo);
        });
        return questionReviewVos;
    }

    public AiEvaluationResult fetchAiResult(QuestionAiEvaluation questionAiEvaluation) {
        AiEvaluationResult aiResult = new AiEvaluationResult();
        aiResult.setScoreRate(questionAiEvaluation.getScoreRate());
        aiResult.setSummary(questionAiEvaluation.getSummary());
        aiResult.setAccurateComment(questionAiEvaluation.getAccurateComment());
        aiResult.setInnovativeComment(questionAiEvaluation.getInnovativeComment());
        aiResult.setMissingComment(questionAiEvaluation.getMissingComment());
        aiResult.setWrongComment(questionAiEvaluation.getWrongComment());
        aiResult.setModelName(questionAiEvaluation.getModelName());
        return aiResult;
    }


    @Override
    public void saveUserQuestionNote(UserQuestionNoteDTO noteDTO) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (noteDTO == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //用户在笔记里输入 "  "空格字符，是被允许的，null和""空字符串（长度为0）的不行
        if (noteDTO.getBankId() == null || noteDTO.getQuestionId() == null || noteDTO.getNoteContent() == null || noteDTO.getNoteContent().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(noteDTO.getBankId());

        // 变更：笔记保存前直接检查两类题目实体的 bankId + questionId，不再查询关系表。
        Long interviewCount = interviewQuestionInfoMapper.selectCount(
                new LambdaQueryWrapper<InterviewQuestionInfo>()
                        .eq(InterviewQuestionInfo::getBankId, noteDTO.getBankId())
                        .eq(InterviewQuestionInfo::getId, noteDTO.getQuestionId())
        );
        Long certificateCount = certificateQuestionInfoMapper.selectCount(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .eq(CertificateQuestionInfo::getBankId, noteDTO.getBankId())
                        .eq(CertificateQuestionInfo::getId, noteDTO.getQuestionId())
        );
        if (interviewCount + certificateCount == 0) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR); //PARAM_ERROR = 前端传参问题；
        }

        Long userId = LoginUserHolder.getUserId();
        UserQuestionNote userQuestionNote = new UserQuestionNote();
        userQuestionNote.setUserId(userId);
        userQuestionNote.setBankId(noteDTO.getBankId());
        userQuestionNote.setQuestionId(noteDTO.getQuestionId()); //空格字符串可以保存，但null和空字符串不行
        userQuestionNote.setNoteContent(noteDTO.getNoteContent());
        userQuestionNoteMapper.insert(userQuestionNote);
    }

    @Override
    public List<CertificateQuestionPageVO> getCertificateByBankId(Long bankId) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);
        // 认证题列表直接按 bank_id 查询，并使用题目表 question_no 升序。
        LambdaQueryWrapper<CertificateQuestionInfo> certificateQueryWrapper = new LambdaQueryWrapper<>();
        certificateQueryWrapper.eq(CertificateQuestionInfo::getBankId, bankId)
                .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE)
                .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .orderByAsc(CertificateQuestionInfo::getQuestionNo)
                .orderByAsc(CertificateQuestionInfo::getId);

        List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(certificateQueryWrapper);
        if (certificateQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        List<Long> questionIds = certificateQuestionInfos.stream().map(CertificateQuestionInfo::getId).toList();

        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, questionIds);

        List<CertificateQuestionPageVO> certificateQuestionPageVos = new ArrayList<>();
        certificateQuestionInfos.forEach(certificateQuestionInfo -> {
            CertificateQuestionPageVO vo = new CertificateQuestionPageVO();

            vo.setQuestionId(certificateQuestionInfo.getId());
            vo.setTitle(certificateQuestionInfo.getTitle());
            vo.setOptions(certificateQuestionInfo.getOptions());
            vo.setQuestionType(certificateQuestionInfo.getQuestionType());
            vo.setImageUrl(readUrlSigner.sign(certificateQuestionInfo.getImageObjectKey()));
            vo.setIsFavorite(favoriteQuestionMap.containsKey(certificateQuestionInfo.getId()));
            certificateQuestionPageVos.add(vo);
        });
        certificateQuestionInfoMapper.certificateViewCount(bankId);
        return certificateQuestionPageVos;

    }

    @Transactional
    @Override
    public CertificateAnswerPageVO getCertificateAnswer(CertificateQuestionSubmitDTO submitDTO) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (submitDTO == null || submitDTO.getQuestionId() == null || submitDTO.getQuestionType() == null ||
                submitDTO.getChosenOptions() == null || submitDTO.getBankId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long questionId = submitDTO.getQuestionId();
        Long bankId = submitDTO.getBankId();
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 变更：提交答案时直接在认证题查询中校验 bankId，阻止跨题库提交。
        LambdaQueryWrapper<CertificateQuestionInfo> certificateQueryWrapper = new LambdaQueryWrapper<>();
        certificateQueryWrapper.eq(CertificateQuestionInfo::getId, questionId)
                .eq(CertificateQuestionInfo::getBankId, bankId)
                .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .eq(CertificateQuestionInfo::getQuestionType, submitDTO.getQuestionType());

        CertificateQuestionInfo certificateQuestionInfo = certificateQuestionInfoMapper.selectOne(certificateQueryWrapper);
        if (certificateQuestionInfo == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> questionIdList = List.of(questionId);
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, questionIdList);

        if (certificateQuestionInfo.getCorrectAnswer() == null || certificateQuestionInfo.getCorrectAnswer().isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<String> correctAnswers = certificateQuestionInfo.getCorrectAnswer();
        List<String> chosenOptions = submitDTO.getChosenOptions();

        boolean correct;
        if (chosenOptions == null) {
            correct = false;
        }else {
            // HashSet.equals() 会比较两个集合包含的元素是否完全相同。
            // 元素是 String 时，集合内部会使用 String.equals() 比较字符串内容。
            // 先用 size 判断 用户选项和正确答案的数量上是否一致
            // 再忽略答案的顺序，比较用户选项和正确答案的内容是否一致（使用HashSet去重之后）
            // 二者缺一不可，如果只是HashSet去重+比较内容，会忽略掉用户可能重复传的数据（虽然这也不太可能发生）
            correct = chosenOptions.size() == correctAnswers.size() && new HashSet<>(chosenOptions).equals(new HashSet<>(correctAnswers));
        }

        //依旧是要把用户作答记录保存到UserQuestionAnswer
        Long userId = LoginUserHolder.getUserId();

        UserQuestionAnswer userQuestionAnswer = new UserQuestionAnswer();
        userQuestionAnswer.setUserId(userId);
        userQuestionAnswer.setBankId(bankId);
        userQuestionAnswer.setQuestionId(questionId);
        userQuestionAnswer.setQuestionType(certificateQuestionInfo.getQuestionType());
        userQuestionAnswer.setChosenOptions(submitDTO.getChosenOptions());
        userQuestionAnswer.setIsCorrect(correct);
        userQuestionAnswer.setAnsweredTime(LocalDateTime.now());
        saveOrUpdateLatestAnswer(userQuestionAnswer);

        CertificateAnswerPageVO answer = new CertificateAnswerPageVO();
        answer.setCorrectAnswer(certificateQuestionInfo.getCorrectAnswer());
        answer.setAnalysis(certificateQuestionInfo.getAnalysis());
        answer.setQuestionId(questionId);
        answer.setCorrect(correct);
        answer.setIsFavorite(favoriteQuestionMap.containsKey(questionId));
        return answer;
    }

    @Transactional
    @Override
    //用户每次“进入或重新进入题库页面”时，前端都应该执行一次初始化查询：
    //有答题记录，reload；没有答题记录，正常返回 CertificateQuestionPageVO
    public List<CertificateQuestionReviewVO> getCertificateRecord(Long bankId) {
        //校验是否是会员，不是则抛异常
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 变更：认证题作答记录页直接按题目 bank_id 查询，不再先构造关系 ID 集合。
        LambdaQueryWrapper<CertificateQuestionInfo> certificateQueryWrapper = new LambdaQueryWrapper<>();
        certificateQueryWrapper.eq(CertificateQuestionInfo::getBankId, bankId)
                .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE)
                .orderByAsc(CertificateQuestionInfo::getQuestionNo)
                .orderByAsc(CertificateQuestionInfo::getId);

        List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(certificateQueryWrapper);
        if (certificateQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        List<Long> certificateQuestionIds = certificateQuestionInfos.stream().map(CertificateQuestionInfo::getId).toList();
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, certificateQuestionIds);
        Map<Long, CertificateQuestionInfo> certificateQuestionMap = certificateQuestionInfos.stream()
                .collect(Collectors.toMap(CertificateQuestionInfo::getId, certificateQuestionInfo -> certificateQuestionInfo));


        Long userId = LoginUserHolder.getUserId();
        LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userAnswerQueryWrapper.eq(UserQuestionAnswer::getBankId, bankId)
                .in(UserQuestionAnswer::getQuestionId, certificateQuestionIds)
                .eq(UserQuestionAnswer::getUserId, userId)
                .in(UserQuestionAnswer::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);

        List<UserQuestionAnswer> userQuestionAnswers = userQuestionAnswerMapper.selectList(userAnswerQueryWrapper);
        if (userQuestionAnswers.isEmpty()) {
            return List.of(); //用户一道题都没做，那么就返回空列表
        }
        List<CertificateQuestionReviewVO> questionReviewVos = new ArrayList<>();
        userQuestionAnswers.forEach(userQuestionAnswer -> {
            CertificateQuestionReviewVO vo = new CertificateQuestionReviewVO();
            CertificateQuestionInfo certificateQuestionInfo = certificateQuestionMap.get(userQuestionAnswer.getQuestionId());

            vo.setQuestionId(userQuestionAnswer.getQuestionId());
            vo.setTitle(certificateQuestionInfo.getTitle());
            vo.setOptions(certificateQuestionInfo.getOptions());
            vo.setQuestionType(certificateQuestionInfo.getQuestionType());
            vo.setCorrectAnswer(certificateQuestionInfo.getCorrectAnswer());
            vo.setAnalysis(certificateQuestionInfo.getAnalysis());
            vo.setImageUrl(readUrlSigner.sign(certificateQuestionInfo.getImageObjectKey()));
            vo.setIsCorrect(userQuestionAnswer.getIsCorrect());
            vo.setIsFavorite(favoriteQuestionMap.containsKey(userQuestionAnswer.getQuestionId()));
            vo.setChosenOptions(userQuestionAnswer.getChosenOptions());
            questionReviewVos.add(vo);
        });
        return questionReviewVos;
        /*
        前端代码：
        const [questions, answeredRecords] = await Promise.all([
            getCertificateQuestions(bankId),
            getCertificateRecordReview(bankId)
        ]);
        answeredMap 只是用来根据 questionId 快速查找答案记录，不负责决定题目顺序。
        前端按照 questions 遍历。

        const answeredMap = new Map(
            answeredRecords.map(item => [item.questionId, item])
        );
        const questionStates = questions.map(question => {
            const answeredRecord = answeredMap.get(question.questionId);

            return answeredRecord
                ? { ...question, ...answeredRecord, answered: true }
                : { ...question, answered: false };
        });

        前端展示逻辑：
        answeredMap 中存在该 questionId：展示 CertificateQuestionReviewVO。
        不存在：展示原来的 CertificateQuestionPageVO。
         */
    }

    @Transactional
    @Override
    public List<InterviewQuestionReviewVO> getInterviewRecord(Long bankId) {
        //校验是否是会员，不是则抛异常
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 变更：面试题作答记录页直接按 bank_id 查询，并保留人工拖拽顺序。
        LambdaQueryWrapper<InterviewQuestionInfo> interviewQueryWrapper = new LambdaQueryWrapper<>();
        interviewQueryWrapper.eq(InterviewQuestionInfo::getBankId, bankId)
                .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .in(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY)
                .orderByAsc(InterviewQuestionInfo::getQuestionNo)
                .orderByAsc(InterviewQuestionInfo::getId);

        List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewQueryWrapper);
        if (interviewQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> interviewQuestionIds = interviewQuestionInfos.stream().map(InterviewQuestionInfo::getId).toList();
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, interviewQuestionIds);
        Map<Long, InterviewQuestionInfo> questionInfoMap = interviewQuestionInfos.stream().
                collect(Collectors.toMap(InterviewQuestionInfo::getId, interviewQuestionInfo -> interviewQuestionInfo));

        Long userId = LoginUserHolder.getUserId();
        //再看userId之下，都答了哪些题
        LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userAnswerQueryWrapper.eq(UserQuestionAnswer::getBankId, bankId)
                .in(UserQuestionAnswer::getQuestionId, interviewQuestionIds)
                .eq(UserQuestionAnswer::getUserId, userId)
                .in(UserQuestionAnswer::getQuestionType, QuestionInfoQuestionType.ESSAY);

        List<UserQuestionAnswer> userQuestionAnswers = userQuestionAnswerMapper.selectList(userAnswerQueryWrapper);

        //如果一道题都没答，返回空列表即可
        if (userQuestionAnswers.isEmpty()) {
            return List.of();
        }

        List<Long> userAnswerIds = userQuestionAnswers.stream().map(UserQuestionAnswer::getId).toList();

        //再接着查QuestionAiEvaluation
        LambdaQueryWrapper<QuestionAiEvaluation> questionAiEvaluationQueryWrapper = new LambdaQueryWrapper<>();
        questionAiEvaluationQueryWrapper.in(QuestionAiEvaluation::getQuestionId, interviewQuestionIds)
                .in(QuestionAiEvaluation::getId, userAnswerIds)
                .eq(QuestionAiEvaluation::getUserId, userId);

        List<QuestionAiEvaluation> questionAiEvaluations = questionAiEvaluationMapper.selectList(questionAiEvaluationQueryWrapper);
        Map<Long, QuestionAiEvaluation> aiEvaluationMap = questionAiEvaluations.stream()
                .collect(Collectors.toMap(QuestionAiEvaluation::getAnswerId, questionAiEvaluation -> questionAiEvaluation));


        //如果答了，那么就开始遍历
        List<InterviewQuestionReviewVO> questionReviewVos = new ArrayList<>();
        userQuestionAnswers.forEach(userQuestionAnswer -> {
            InterviewQuestionReviewVO questionReviewVo = new InterviewQuestionReviewVO();
            InterviewQuestionInfo interviewQuestionInfo = questionInfoMap.get(userQuestionAnswer.getQuestionId());
            QuestionAiEvaluation questionAiEvaluation = aiEvaluationMap.get(userQuestionAnswer.getId());

            questionReviewVo.setQuestionId(interviewQuestionInfo.getId());
            questionReviewVo.setTitle(interviewQuestionInfo.getTitle());
            questionReviewVo.setImageUrl(readUrlSigner.sign(interviewQuestionInfo.getImageObjectKey()));
            questionReviewVo.setQuestionType(userQuestionAnswer.getQuestionType());
            questionReviewVo.setAnalysis(interviewQuestionInfo.getAnalysis());
            questionReviewVo.setContent(userQuestionAnswer.getContent());
            questionReviewVo.setIsFavorite(favoriteQuestionMap.containsKey(interviewQuestionInfo.getId()));
            if (questionAiEvaluation != null) {
                AiEvaluationResult aiResult = fetchAiResult(questionAiEvaluation);
                questionReviewVo.setAiResult(aiResult);
                questionReviewVo.setIsCorrect(aiResult.getScoreRate().compareTo(BigDecimal.valueOf(60)) >= 0);
            }
            questionReviewVos.add(questionReviewVo);
        });
        return questionReviewVos;
        /*
        同样的：
        前端展示逻辑：
        answeredMap 中存在该 questionId：展示 InterviewQuestionReviewVO。
        不存在：展示原来的 InterviewQuestionPageVO。

        进入题库：请求后端并建立 answeredMap
        点击题目：读取 answeredMap
        提交答案：更新 answeredMap
        退出后重新进入或刷新：再次请求后端恢复 answeredMap

        排序在
         */
    }

    @Override
    public void clearRecord(Long bankId, GroupType groupType) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        if (groupType.equals(GroupType.CERTIFICATION)) {
            // 变更：清除记录时直接按认证题 bank_id 取得当前题库题目。
            LambdaQueryWrapper<CertificateQuestionInfo> certificateQueryWrapper = new LambdaQueryWrapper<>();
            certificateQueryWrapper.eq(CertificateQuestionInfo::getBankId, bankId)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE)
                    .orderByAsc(CertificateQuestionInfo::getQuestionNo)
                    .orderByAsc(CertificateQuestionInfo::getId);

            List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(certificateQueryWrapper);
            if (certificateQuestionInfos.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            List<Long> certificateIds = certificateQuestionInfos.stream().map(CertificateQuestionInfo::getId).toList();

            Long userId = LoginUserHolder.getUserId();
            LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
            userAnswerQueryWrapper.eq(UserQuestionAnswer::getBankId, bankId)
                    .eq(UserQuestionAnswer::getUserId, userId)
                    .in(UserQuestionAnswer::getQuestionId, certificateIds);

            userQuestionAnswerMapper.delete(userAnswerQueryWrapper);//再做一个定时任务，物理删除
        }

        if(groupType.equals(GroupType.INTERVIEW)){
            // 变更：面试题同样直接按 bank_id 查询，不再依赖关系 ID 列表。
            LambdaQueryWrapper<InterviewQuestionInfo> interviewQueryWrapper = new LambdaQueryWrapper<>();
            interviewQueryWrapper.eq(InterviewQuestionInfo::getBankId, bankId)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .in(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY)
                    .orderByAsc(InterviewQuestionInfo::getQuestionNo)
                    .orderByAsc(InterviewQuestionInfo::getId);

            List<InterviewQuestionInfo> interviewQuestionInfos = interviewQuestionInfoMapper.selectList(interviewQueryWrapper);
            if (interviewQuestionInfos.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            List<Long> interviewIds = interviewQuestionInfos.stream().map(InterviewQuestionInfo::getId).toList();

            Long userId = LoginUserHolder.getUserId();
            LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
            userAnswerQueryWrapper.eq(UserQuestionAnswer::getBankId, bankId)
                    .eq(UserQuestionAnswer::getUserId, userId)
                    .in(UserQuestionAnswer::getQuestionId, interviewIds);

            userQuestionAnswerMapper.delete(userAnswerQueryWrapper);
        }
    }

    @Transactional
    @Override
    public void collect(Long bankId, Long questionId, ActionStatus actionStatus) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null || questionId == null || actionStatus == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        Long userId = LoginUserHolder.getUserId();
        // 变更：收藏前直接检查两类题目实体的归属，防止收藏其他题库的题目。
        Long interviewCount = interviewQuestionInfoMapper.selectCount(
                new LambdaQueryWrapper<InterviewQuestionInfo>()
                        .eq(InterviewQuestionInfo::getBankId, bankId)
                        .eq(InterviewQuestionInfo::getId, questionId)
        );
        Long certificateCount = certificateQuestionInfoMapper.selectCount(
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .eq(CertificateQuestionInfo::getBankId, bankId)
                        .eq(CertificateQuestionInfo::getId, questionId)
        );
        if (interviewCount + certificateCount == 0) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //关键点在这一步，加一个数据库锁 操作，解决并发问题
        //并且查询 is_deleted = 1 的历史数据
        //因为如果你直接使用 LambdaQueryWrapper，无法查询 is_deleted = 1 的历史数据，因为Mybatis-plus默认 is_deleted = 0
        //FOR UPDATE 倒是可以通过 LambdaQueryWrapper 实现，即 .last("FOR UPDATE")
        //所以调用mapper方法的主要目的是为了加锁，防止并发问题 + 能查询 逻辑删除的 历史数据
        UserFavoriteQuestion existing = userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(userId, bankId, questionId);

        if (existing == null) {
            if (actionStatus == ActionStatus.ACTIVATE) {
                UserFavoriteQuestion favorite = new UserFavoriteQuestion();
                favorite.setUserId(userId);
                favorite.setBankId(bankId);
                favorite.setQuestionId(questionId);
                favorite.setCollectedTime(LocalDateTime.now());
                int result = userFavoriteQuestionMapper.insert(favorite);
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                }
            }
        } else if (existing != null && Boolean.TRUE.equals(existing.getDeleted())) {
            if (actionStatus == ActionStatus.ACTIVATE) {
                int result = userFavoriteQuestionMapper.restoreById(existing.getId());
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                }
            }
        } else {
            if (actionStatus == ActionStatus.DEACTIVATE) {
                int result = userFavoriteQuestionMapper.deleteById(existing.getId());
                if (result != 1) {
                    throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
                }
            }
        }
        /*
        你不需要更新 点赞/收藏/转发 的数量，所以不需要 changed 标识 和 数据库原子操作
         */
    }

    @Transactional
    @Override
    public List<CertificateQuestionReviewVO> getCertificateQuestionReview(Long bankId) {
        //校验是否是会员，不是则抛异常
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());
        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 变更：完成题库回顾直接按认证题 bank_id 查询，并使用手动顺序。
        LambdaQueryWrapper<CertificateQuestionInfo> certificateQueryWrapper = new LambdaQueryWrapper<>();
        certificateQueryWrapper.eq(CertificateQuestionInfo::getBankId, bankId)
                .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE)
                .orderByAsc(CertificateQuestionInfo::getQuestionNo)
                .orderByAsc(CertificateQuestionInfo::getId);

        List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(certificateQueryWrapper);
        if (certificateQuestionInfos.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> certificateIds = certificateQuestionInfos.stream().map(CertificateQuestionInfo::getId).toList();
        Map<Long, UserFavoriteQuestion> favoriteQuestionMap = getFavoriteQuestionMap(bankId, certificateIds);

        Long userId = LoginUserHolder.getUserId();
        LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
        userAnswerQueryWrapper.eq(UserQuestionAnswer::getBankId, bankId)
                .eq(UserQuestionAnswer::getUserId, userId)
                .in(UserQuestionAnswer::getQuestionId, certificateIds);

        List<UserQuestionAnswer> userQuestionAnswers = userQuestionAnswerMapper.selectList(userAnswerQueryWrapper);
        //允许列表为空，那就是一道题没做就提交答案了

        //设计成Map集合，就不用每一次都QueryWrapper查询了，提高性能
        Map<Long, UserQuestionAnswer> questionAnswerMap = userQuestionAnswers.stream()
                .collect(Collectors.toMap(UserQuestionAnswer::getQuestionId, userQuestionAnswer -> userQuestionAnswer));


        //开始组装 List<InterviewQuestionReviewVO>
        List<CertificateQuestionReviewVO> questionReviewVos = new ArrayList<>();
        certificateQuestionInfos.forEach(questionInfo -> {
            CertificateQuestionReviewVO vo = new CertificateQuestionReviewVO();
            UserFavoriteQuestion userFavoriteQuestion = favoriteQuestionMap.get(questionInfo.getId());

            vo.setQuestionId(questionInfo.getId());
            vo.setTitle(questionInfo.getTitle());
            vo.setOptions(questionInfo.getOptions());
            vo.setCorrectAnswer(questionInfo.getCorrectAnswer());
            vo.setAnalysis(questionInfo.getAnalysis());
            vo.setQuestionType(questionInfo.getQuestionType());
            vo.setImageUrl(readUrlSigner.sign(questionInfo.getImageObjectKey()));
            vo.setIsFavorite(userFavoriteQuestion != null);
            UserQuestionAnswer userQuestionAnswer = questionAnswerMap.get(questionInfo.getId());
            if (userQuestionAnswer != null) {
                vo.setChosenOptions(userQuestionAnswer.getChosenOptions());
                vo.setIsCorrect(userQuestionAnswer.getIsCorrect());
            }
            questionReviewVos.add(vo);
        });
        return questionReviewVos;
    }

    //点击“提交试卷”/“完成题库”按钮后，调用此接口方法
    //返回 题库正确率 + 题库中题目全套信息的列表（做过的+没做过的）
    @Override
    public BankFinishVO finishBank(Long bankId, GroupType groupType) {
        membershipAccessService.requireActiveMembership(LoginUserHolder.getUserId());

        BankFinishVO finishVO = new BankFinishVO();
        if (groupType.equals(GroupType.INTERVIEW)) {
            List<InterviewQuestionReviewVO> interviewQuestionReviewVos = getInterviewQuestionReview(bankId);
            finishVO.setInterviewQuestionReviewVos(interviewQuestionReviewVos);
            int count = interviewQuestionInfoMapper.bankCompletionCount(bankId);
            if(count != 1){
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
        }

        if (groupType.equals(GroupType.CERTIFICATION)) {
            List<CertificateQuestionReviewVO> certificateQuestionReviewVos = getCertificateQuestionReview(bankId);
            finishVO.setCertificateQuestionReviewVos(certificateQuestionReviewVos);
            int count = certificateQuestionInfoMapper.bankCompletionCount(bankId);
            if(count != 1){
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }

        }

        QuestionCountVO questionCountVO = buildQuestionCountVO(bankId, groupType);
        finishVO.setQuestionCount(questionCountVO);

        return finishVO;
    }

    public QuestionCountVO buildQuestionCountVO(Long bankId, GroupType groupType) {

        QuestionCountVO countVO = new QuestionCountVO();

        // 1. 参数校验：bankId 不能为空。
        // bankId 表示用户当前完成的是哪一个题库。
        if (bankId == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        // 2. 获取当前登录用户的 userId。
        // 这个 userId 是登录拦截器从 token 中解析后放进 ThreadLocal 的。
        // 后面查询时必须带上 userId，防止用户查看别人的练习统计。
        Long userId = LoginUserHolder.getUserId();

        if (groupType.equals(GroupType.CERTIFICATION)) {
            // 变更：统计认证题数量时直接使用题目表 bank_id。
            LambdaQueryWrapper<CertificateQuestionInfo> certificateQueryWrapper = new LambdaQueryWrapper<>();
            certificateQueryWrapper.eq(CertificateQuestionInfo::getBankId, bankId)
                    .in(CertificateQuestionInfo::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE)
                    .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED);

            Long totalCount = certificateQuestionInfoMapper.selectCount(certificateQueryWrapper);
            if (totalCount == 0) {
                countVO.setAnsweredCount(0L);
                countVO.setCorrectCount(0L);
                countVO.setCorrectRate(BigDecimal.ZERO);
                return countVO;
            }

            List<CertificateQuestionInfo> certificateQuestionInfos = certificateQuestionInfoMapper.selectList(certificateQueryWrapper);
            if(certificateQuestionInfos.isEmpty()){
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
            List<Long> certificateIds = certificateQuestionInfos.stream().map(CertificateQuestionInfo::getId).toList();

            countVO.setTotalCount(totalCount);

            LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
            userAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId, userId)
                    .eq(UserQuestionAnswer::getBankId, bankId)
                    .in(UserQuestionAnswer::getQuestionId, certificateIds)
                    .in(UserQuestionAnswer::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE);

            Long answeredCount = userQuestionAnswerMapper.selectCount(userAnswerQueryWrapper);
            countVO.setAnsweredCount(answeredCount);

            LambdaQueryWrapper<UserQuestionAnswer> correctAnswerQueryWrapper = new LambdaQueryWrapper<>();
            correctAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId, userId)
                    .eq(UserQuestionAnswer::getBankId, bankId)
                    .in(UserQuestionAnswer::getQuestionType, QuestionInfoQuestionType.SINGLE_CHOICE, QuestionInfoQuestionType.MULTIPLE)
                    .eq(UserQuestionAnswer::getIsCorrect, true);
            Long correctCount = userQuestionAnswerMapper.selectCount(correctAnswerQueryWrapper);

            // 9. 设置用户答对的题目数量。
            countVO.setCorrectCount(correctCount);

            // 10. 计算正确率。
            // 这里用 correctCount / totalCount，表示整套题的正确率。
            BigDecimal correctRate = BigDecimal.valueOf(correctCount)
                    .divide(
                            BigDecimal.valueOf(totalCount),
                            2,
                            RoundingMode.HALF_UP
                    );
            countVO.setCorrectRate(correctRate);

            UserBankCorrectRate bankCorrectRate = new UserBankCorrectRate();
            bankCorrectRate.setUserId(userId);
            bankCorrectRate.setBankId(bankId);
            bankCorrectRate.setGroupType(groupType);
            bankCorrectRate.setCorrectRate(countVO.getCorrectRate());
            userBankCorrectRateMapper.insert(bankCorrectRate);
        }

        //返回的是用户作答的interview题库的题目的平均正确率
        if (groupType.equals(GroupType.INTERVIEW)) {
            // 变更：统计面试题数量时直接使用题目表 bank_id。
            LambdaQueryWrapper<InterviewQuestionInfo> interviewQueryWrapper = new LambdaQueryWrapper<>();
            interviewQueryWrapper.eq(InterviewQuestionInfo::getBankId, bankId)
                    .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED)
                    .eq(InterviewQuestionInfo::getQuestionType, QuestionInfoQuestionType.ESSAY);
            Long totalCount = interviewQuestionInfoMapper.selectCount(interviewQueryWrapper);
            List<Long> interviewIds = interviewQuestionInfoMapper.selectList(interviewQueryWrapper).stream().map(InterviewQuestionInfo::getId).toList();

            countVO.setTotalCount(totalCount);

            LambdaQueryWrapper<UserQuestionAnswer> userAnswerQueryWrapper = new LambdaQueryWrapper<>();
            userAnswerQueryWrapper.eq(UserQuestionAnswer::getUserId, userId)
                    .eq(UserQuestionAnswer::getBankId, bankId)
                    .in(UserQuestionAnswer::getQuestionId, interviewIds)
                    .eq(UserQuestionAnswer::getQuestionType, QuestionInfoQuestionType.ESSAY);

            Long answeredCount = userQuestionAnswerMapper.selectCount(userAnswerQueryWrapper);
            countVO.setAnsweredCount(answeredCount);

            List<BigDecimal> aiScores = userQuestionAnswerMapper.selectList(userAnswerQueryWrapper)
                    .stream()
                    .map(UserQuestionAnswer::getAiScoreRate)
                    .filter(Objects::nonNull)
                    .toList();
            if (answeredCount == 0) {
                countVO.setCorrectRate(BigDecimal.ZERO);
            } else if (!aiScores.isEmpty()) {
                BigDecimal sum = aiScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                countVO.setCorrectRate(sum.divide(BigDecimal.valueOf(aiScores.size()), 2, RoundingMode.HALF_UP));
            }
            UserBankCorrectRate bankCorrectRate = new UserBankCorrectRate();
            bankCorrectRate.setUserId(userId);
            bankCorrectRate.setBankId(bankId);
            bankCorrectRate.setGroupType(groupType);
            bankCorrectRate.setCorrectRate(countVO.getCorrectRate());
            userBankCorrectRateMapper.insert(bankCorrectRate);
        }
        return countVO;
    }


    @Override
    public AiChatVO startAiChat(Long bankId, GroupType groupType) {
        //校验是否是会员，不是则抛异常
        membershipAccessService.requirePremiumPlus(LoginUserHolder.getUserId());
        // 这个接口用于用户点击“追问AI”按钮时，先查询当前题库下已有的历史会话。
        if (bankId == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(bankId);

        Long userId = LoginUserHolder.getUserId();

        // 查询 userId下的 bankId，是否有session
        AiChatSession session = aiChatSessionMapper.selectOne(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getBankId, bankId)
        );

        if (session == null) {//没有session，创建一个session
            AiChatSession newSession = new AiChatSession();
            newSession.setUserId(userId);
            newSession.setBankId(bankId);
            newSession.setGroupType(groupType);
            newSession.setStatus(AiChatSessionStatus.ACTIVE);
            aiChatSessionMapper.insert(newSession);
            session = newSession;


            //还要创建一个AiChatMessage，因为你要写一个第一次的欢迎语
            AiChatMessage welcomeMessage = new AiChatMessage();
            welcomeMessage.setSessionId(newSession.getId());
            welcomeMessage.setGroupType(groupType);
            welcomeMessage.setMessageContent("你好，有关于这道题的知识点想深入了解吗？");
            welcomeMessage.setSenderType(AiChatMessageSenderType.AI);
            welcomeMessage.setModelName(RULE_WELCOME_MODEL);
            aiChatMessageMapper.insert(welcomeMessage);
        }

        if (session.getStatus().equals(AiChatSessionStatus.CLOSED)) {
            //注意：session不删除，它只是一个分类，跟你的group和module一样，它不是容器，实体类里不存放AiChatMessage
            //所以，重新把状态打开即可
            session.setStatus(AiChatSessionStatus.ACTIVE);
            aiChatSessionMapper.updateById(session);

            LambdaQueryWrapper<AiChatMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiChatMessage::getSessionId, session.getId());
            aiChatMessageMapper.delete(queryWrapper); //逻辑删除：根据sessionId查询出的所有message
            //重新创建第一条message（欢迎语）
            //每一条实际的消息，是存放在AiChatMessage表中
            AiChatMessage welcomeMessage = new AiChatMessage();
            welcomeMessage.setSessionId(session.getId());
            welcomeMessage.setQuestionId(null);
            welcomeMessage.setGroupType(groupType);
            welcomeMessage.setMessageContent("你好，有关于这道题的知识点想深入了解吗？");
            welcomeMessage.setSenderType(AiChatMessageSenderType.AI);
            welcomeMessage.setModelName(RULE_WELCOME_MODEL);
            aiChatMessageMapper.insert(welcomeMessage);
        }

        return buildAiChatVO(session); //把 保存到AiChatVO 的动作抽象到一个方法中
    }

    private AiChatVO buildAiChatVO(AiChatSession session) {
        AiChatVO vo = new AiChatVO();
        vo.setSessionId(session.getId());
        vo.setBankId(session.getBankId());

        List<AiChatMessage> aiChatMessages = aiChatMessageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .orderByAsc(AiChatMessage::getId)
        );
        List<AiChatMessageVO> messageVos = new ArrayList<>();

        aiChatMessages.forEach(aiChatMessage -> {
            AiChatMessageVO messageVo = new AiChatMessageVO();
            messageVo.setMessageId(aiChatMessage.getId());
            messageVo.setSenderType(aiChatMessage.getSenderType());
            messageVo.setMessageContent(aiChatMessage.getMessageContent());
            messageVo.setCreatedTime(aiChatMessage.getCreatedTime()); //AI弹窗是需要显示会话时间的
            messageVos.add(messageVo);
        });
        vo.setMessages(messageVos);
        return vo;
    }


    @Transactional
    @Override
    public AiChatVO followUpAi(AiFollowUpDTO dto) { //真正发送问题时创建 session
        membershipAccessService.requirePremiumPlus(LoginUserHolder.getUserId());
        // 1. 校验追问请求。bankId 决定复用哪个 AI 会话，questionId + bankType 决定本轮追问取哪道题的解析。
        if (dto == null
                || dto.getBankId() == null
                || dto.getQuestionId() == null
                || dto.getGroupType() == null
                || dto.getMessage() == null
                || dto.getMessage().isBlank()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        publishedQuestionBankAccessService.requirePublished(dto.getBankId());

        // 2. 获取当前登录用户。AI 会话必须归属于用户，不能让用户读到别人的追问记录。
        Long userId = LoginUserHolder.getUserId();

        // 3. 获取这个用户在当前题库下的 AI 会话。
        // 同一个 userId + bankId 只保留一个会话，所以用户切到下一题时仍然能看到原会话。
        AiChatSession session = aiChatSessionMapper.selectOne(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getBankId, dto.getBankId())
                        .eq(AiChatSession::getStatus, AiChatSessionStatus.ACTIVE)
        );
        if (session == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }


        // 4. 查询本次提问之前已经存在的历史消息，用来给大模型提供上下文。
        List<AiChatMessage> history = aiChatMessageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .orderByAsc(AiChatMessage::getId)
        );

        // 5. 构造题目上下文和最终 prompt。
        // questionContext 是当前用户停留题目的题目内容 + 答案解析（字符串）
        String questionContext = aiPromptBuilder.buildQuestionContext(dto);

        //questionContext 是当前用户停留题目的题目内容 + 答案解析（字符串）
        //history 是这个session内的历史收发信息
        //message 是本次用户输入的问题
        //将这三个部分，通过格式化，一次性喂给AI
        String prompt = aiPromptBuilder.buildAiChatPrompt(questionContext, history, dto.getMessage());

        // 6. 保存用户这次输入的追问。
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setQuestionId(dto.getQuestionId());
        userMessage.setGroupType(dto.getGroupType());
        userMessage.setSenderType(AiChatMessageSenderType.USER);
        userMessage.setMessageContent(dto.getMessage()); //一条来自用户输入的信息，保存到ai_chat_message表
        aiChatMessageMapper.insert(userMessage);

        // 7. 调用大模型生成回复（真正使用AI生成回复的地方）
        LlmResponse llmResponse = llmClient.chat(prompt);

        // 8. 保存 AI 回复。这样下一次打开“追问AI”弹窗时，可以恢复完整上下文。
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(session.getId());
        aiMessage.setQuestionId(dto.getQuestionId());
        aiMessage.setGroupType(dto.getGroupType());
        aiMessage.setSenderType(AiChatMessageSenderType.AI);
        aiMessage.setMessageContent(llmResponse.content()); //一条来自AI回复的消息，也保存到ai_chat_message表
        aiMessage.setModelName(llmResponse.modelName());
        aiChatMessageMapper.insert(aiMessage);

        // 9. 返回最新完整会话，前端可以直接用 messages 渲染弹窗。
        return buildAiChatVO(session);
    }

    @Override
    public void closeAiChat(Long bankId) {//前端要设计调用这个方法的逻辑
        if (bankId == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long userId = LoginUserHolder.getUserId();

        AiChatSession session = aiChatSessionMapper.selectOne(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getBankId, bankId)
                        .eq(AiChatSession::getStatus, AiChatSessionStatus.ACTIVE)
        );

        if (session == null) {
            return;
        }

        session.setStatus(AiChatSessionStatus.CLOSED);
        aiChatSessionMapper.updateById(session);
    }


    //查到数据，覆盖；没查到数据，插入
    private Long saveOrUpdateLatestAnswer(UserQuestionAnswer userQuestionAnswer) {
        LambdaQueryWrapper<UserQuestionAnswer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserQuestionAnswer::getUserId, userQuestionAnswer.getUserId())
                .eq(UserQuestionAnswer::getBankId, userQuestionAnswer.getBankId())
                .eq(UserQuestionAnswer::getQuestionId, userQuestionAnswer.getQuestionId());

        UserQuestionAnswer userAnswer = userQuestionAnswerMapper.selectOne(queryWrapper);
        if (userAnswer == null) {
            userQuestionAnswerMapper.insert(userQuestionAnswer);
            return userQuestionAnswer.getId(); //MyBatis-Plus 的 insert(entity) 在自增主键场景下，通常会把数据库生成的 id 自动回填到 entity 里。
        }

        //MyBatis-Plus 的 update 方法没有自动回填 id 功能，所以要手动 set一下
        //根据 where 条件更新已有行，但数据库不会“生成新 id”，MyBatis-Plus 也不会自动查询旧 id 回填
        userQuestionAnswer.setId(userAnswer.getId());
        userQuestionAnswerMapper.updateById(userQuestionAnswer);
        return userQuestionAnswer.getId();
    }

    private void saveOrUpdateLatestEvaluation(QuestionAiEvaluation questionAiEvaluation) {
        LambdaQueryWrapper<QuestionAiEvaluation> evaluationQueryWrapper = new LambdaQueryWrapper<>();
        evaluationQueryWrapper.eq(QuestionAiEvaluation::getAnswerId, questionAiEvaluation.getAnswerId())
                .eq(QuestionAiEvaluation::getQuestionId, questionAiEvaluation.getQuestionId())
                .eq(QuestionAiEvaluation::getUserId, questionAiEvaluation.getUserId());
        QuestionAiEvaluation latestEvaluation = questionAiEvaluationMapper.selectOne(evaluationQueryWrapper);
        if (latestEvaluation == null) {
            questionAiEvaluationMapper.insert(questionAiEvaluation);
        } else {
            questionAiEvaluation.setId(latestEvaluation.getId());
            questionAiEvaluationMapper.updateById(questionAiEvaluation);
        }
    }

    private Map<Long, UserFavoriteQuestion> getFavoriteQuestionMap(Long bankId, Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<UserFavoriteQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFavoriteQuestion::getUserId, LoginUserHolder.getUserId())
                .eq(UserFavoriteQuestion::getBankId, bankId)
                .in(UserFavoriteQuestion::getQuestionId, questionIds);
        List<UserFavoriteQuestion> favorites = userFavoriteQuestionMapper.selectList(queryWrapper);

        return favorites.stream()
                .collect(Collectors.toMap(UserFavoriteQuestion::getQuestionId, Function.identity()));
    }
}
