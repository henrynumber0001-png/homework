package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.*;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.*;
import com.homework.web.app.vo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionInfoServiceImplTest {

    @Mock
    private InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    @Mock
    private UserQuestionAnswerMapper userQuestionAnswerMapper;
    @Mock
    private AiEvaluationService aiEvaluationService;
    @Mock
    private QuestionAiEvaluationMapper questionAiEvaluationMapper;
    @Mock
    private UserQuestionNoteMapper userQuestionNoteMapper;
    @Mock
    private CertificateQuestionInfoMapper certificateQuestionInfoMapper;
    @Mock
    private AiChatSessionMapper aiChatSessionMapper;
    @Mock
    private AiChatMessageMapper aiChatMessageMapper;
    @Mock
    private LlmClient llmClient;
    @Mock
    private AiPromptBuilder aiPromptBuilder;
    @Mock
    private UserFavoriteQuestionMapper userFavoriteQuestionMapper;
    @Mock
    private UserBankCorrectRateMapper userBankCorrectRateMapper;
    @Mock
    private MembershipAccessService membershipAccessService;
    @Mock
    private PublishedQuestionBankAccessService publishedQuestionBankAccessService;
    @Mock
    private CosReadUrlSigner readUrlSigner;

    @InjectMocks
    private QuestionInfoServiceImpl service;

    private final MembershipAccessSnapshot membershipAccessSnapshot = new MembershipAccessSnapshot(
            MembershipStatus.PREMIUM,
            MembershipType.PREMIUM,
            null,
            null
    );

    @BeforeEach
    void setUp() {
        LoginUserHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.removeUserId();
    }

    @Test
    void activateMissingFavoriteInsertsCurrentUserAndBank() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        // Arrange
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(null);

        //如果不配置这一段代码，那么当测试执行到业务代码中的这一步时，Mockito 会返回 int 类型的 默认值 0，然后进入业务代码的抛异常环节
        when(userFavoriteQuestionMapper.insert(any(UserFavoriteQuestion.class))).thenReturn(1);
        //确保insert输入任意的非null的UserFavoriteQuestion对象，都可以正常返回，正常结束方法，而非抛异常

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        //验证注入的依赖是否真的调用了insert方法并传入了指定的 UserFavoriteQuestion对象
        //Stub 可以适当宽松，用于构造测试环境；Assert 和 Verify 应当精确，用于验证业务结果
        verify(userFavoriteQuestionMapper).insert(org.mockito.ArgumentMatchers.<UserFavoriteQuestion>argThat(favorite ->
                favorite.getUserId().equals(7L)
                        && favorite.getBankId().equals(11L)
                        && favorite.getQuestionId().equals(22L)
                        && favorite.getCollectedTime() != null));
    }

    @Test
    void deactivateMissingFavoriteIsIdempotent() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(null);

        service.collect(11L, 22L, ActionStatus.DEACTIVATE);

        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
        verify(userFavoriteQuestionMapper, never()).restoreById(anyLong());
        verify(userFavoriteQuestionMapper, never()).deactivateById(anyLong());
    }

    @Test
    void activateDeletedFavoriteRestoresFavoriteRecordId() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        UserFavoriteQuestion existing = favorite(99L, true);
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);

        //虽然我们做的是单元测试，测的是 QuestionInfoServiceImpl，但是，collect方法的正确执行，离不开这些依赖方法的调用
        //也就是说，当你测试 service.collect()，你到底在测什么？
        //你肯定不仅仅是测这些方法是否有调用这么简单，你还要看看输入特定的参数后，是否返回期待的正确的结果，不然你测啥呢
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(existing);
        //比如，你肯定要写restoreById(99L)，而非restoreById(anyLong())，因为除了id, bankId,userId,questionId 也是 long类型，你如果允许输入anyLong()，你无法检验参数类型是否正确
        when(userFavoriteQuestionMapper.restoreById(99L)).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        //再比如这里，你不仅要 verify restoreById()方法是否执行，你还要 verify 这个restoreById() 的参数是不是 99L，也就是 方法的校验，是要包括对于参数的校验的
        //对于这个校验，就是要看参数是不是 user_favorite_question.id，而非 bankId或者questionId
        verify(userFavoriteQuestionMapper).restoreById(99L);
        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
    }

    @Test
    void deactivateActiveFavoriteUsesFavoriteRecordId() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        UserFavoriteQuestion existing = favorite(99L, false);
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(existing);
        when(userFavoriteQuestionMapper.deleteById(99L)).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.DEACTIVATE);

        verify(userFavoriteQuestionMapper).deleteById(99L);
    }

    @Test
    void repeatedActivateOnActiveFavoriteDoesNotWriteAgain() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L))
                .thenReturn(favorite(99L, false));

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
        verify(userFavoriteQuestionMapper, never()).restoreById(anyLong());
        verify(userFavoriteQuestionMapper, never()).deactivateById(anyLong());
    }

    //单独测的就是 saveOrUpdateLatestAnswer() 方法
    @Test
    void submittingAfterClearRestoresDeletedAnswerInsteadOfInsertingDuplicate() {
        reset(membershipAccessService);
        UserQuestionAnswer deletedAnswer = new UserQuestionAnswer();
        deletedAnswer.setId(99L);
        deletedAnswer.setDeleted(true);
        when(userQuestionAnswerMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L))
                .thenReturn(deletedAnswer);
        when(userQuestionAnswerMapper.restoreById(99L)).thenReturn(1);
        when(userQuestionAnswerMapper.overwriteAllUpdate(any(UserQuestionAnswer.class))).thenReturn(1);

        //你要明白，为什么这个测试方法里，在verify overwriteAllUpdate()方法时，就允许自己 new 一个 UserQuestionAnswer对象，而 getInterviewAnswerTest()就不行呢？
        //因为这里测试的方法是 saveOrUpdateLatestAnswer(userQuestionAnswer)，userQuestionAnswer直接作为一个参数了，完全没有上下文，是完全孤立的一个参数，所以你只能自己 new
        UserQuestionAnswer newAnswer = new UserQuestionAnswer();
        newAnswer.setUserId(7L);
        newAnswer.setBankId(11L);
        newAnswer.setQuestionId(22L);
        newAnswer.setContent("重新作答");

        //等同于 Long answerId = service.saveOrUpdateLatestAnswer(newAnswer);
        Long answerId = ReflectionTestUtils.invokeMethod(
                service, //反射的对象
                "saveOrUpdateLatestAnswer", //反射的对象的方法
                newAnswer //反射的对象的方法的参数
        );

        assertEquals(99L, answerId);
        verify(userQuestionAnswerMapper).restoreById(99L); //在 service层，我不关心你是不是一定返回1，但我关心的是你是否执行了restoreById(99L)，因为这关系到 service层方法的正常运行
        verify(userQuestionAnswerMapper).overwriteAllUpdate(newAnswer);
        assertEquals(99L, newAnswer.getId()); //因为 newAnswer 里没写 answerId，所以要单独校验一下
        verify(userQuestionAnswerMapper, never()).insert(any(UserQuestionAnswer.class));
    }

    @Test
    void interviewQuestionListUsesFavoriteMap() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        //设置一个InterviewQuestionInfo对象
        InterviewQuestionInfo question = new InterviewQuestionInfo();
        question.setId(22L);
        question.setBankId(11L);
        question.setQuestionNo(1);
        question.setTitle("Java 并发");
        question.setImageObjectKey("questions/java-concurrency.png");
        question.setQuestionType(QuestionInfoQuestionType.ESSAY);
        question.setStatus(QuestionInfoStatus.PUBLISHED);

        //设置一个UserFavoriteQuestion对象
        UserFavoriteQuestion favorite = favorite(99L, false);

        when(interviewQuestionInfoMapper.selectList(any())).thenReturn(List.of(question)); //把对象放入List
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favorite)); //把对象放入List
        when(readUrlSigner.sign("questions/java-concurrency.png"))
                .thenReturn("https://cos.example.com/questions/java-concurrency.png?signed=true");

        List<InterviewQuestionPageVO> result = service.getInterviewByBankId(11L);

        assertTrue(result.get(0).getIsFavorite());
        assertEquals(
                "https://cos.example.com/questions/java-concurrency.png?signed=true",
                result.get(0).getImageUrl()
        );
        assertEquals(22L, result.get(0).getQuestionId());
        assertEquals(QuestionInfoQuestionType.ESSAY, result.get(0).getQuestionType());
        assertEquals("Java 并发", result.get(0).getTitle());
        verify(interviewQuestionInfoMapper).incrementViewCount(11L);
    }

    private UserFavoriteQuestion favorite(Long id, boolean deleted) {
        UserFavoriteQuestion favorite = new UserFavoriteQuestion();
        favorite.setId(id);
        favorite.setUserId(7L);
        favorite.setBankId(11L);
        favorite.setQuestionId(22L);
        favorite.setDeleted(deleted);
        return favorite;
    }

    @Test
    public void getInterviewAnswerTest() {

        reset(membershipAccessService);
        MembershipAccessSnapshot premiumPlusSnapshot = new MembershipAccessSnapshot(
                MembershipStatus.PREMIUM_PLUS,
                MembershipType.PREMIUM_PLUS,
                null,
                null);

        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(premiumPlusSnapshot);

        InterviewQuestionSubmitDTO dto = new InterviewQuestionSubmitDTO();
        dto.setBankId(11L);
        dto.setQuestionId(22L);
        dto.setContent("Java并发");

        InterviewQuestionInfo questionInfo = new InterviewQuestionInfo();
        questionInfo.setId(22L);
        questionInfo.setQuestionType(QuestionInfoQuestionType.ESSAY);
        questionInfo.setBankId(11L);
        questionInfo.setTitle("Java 答案");
        questionInfo.setAnalysis("答案");
        questionInfo.setStatus(QuestionInfoStatus.PUBLISHED);

        //service层的测试，是单元测试，所以当需要的返回值是要经过其他 依赖 时，因为我们是模拟的依赖，不是真的通过对方依赖调用数据库并返回数据
        //所以我们必须要预先自己定义一个返回值
        AiEvaluationResult aiResult = new AiEvaluationResult();
        aiResult.setScoreRate(BigDecimal.valueOf(90));


        /**
         * 注意：这里是一个典型的错误，我把它注释掉了，当你再次来review这里，可以快速回顾问题
         * QuestionInfoServiceImpl 第191行：java程序正常执行，是会自己创建QuestionAiEvaluation对象的，不需要你自己 new
         * 因为这些都是 真实程序，不需要 mock依赖，java自己就能搞定，不需要你在测试方法里通过 new对象的方式 标注
         * 而且就算你在测试方法里 new了也没用，真实跑的代码还是java根据代码自己创建的QuestionAiEvaluation对象
         * 你唯一需要做的就是，校验 mock依赖调用的方法、传入的参数，然后针对被测试方法的返回值进行校验
         */
//        QuestionAiEvaluation questionAiEvaluation = new QuestionAiEvaluation();
//        questionAiEvaluation.setScoreRate(BigDecimal.valueOf(90));
//        questionAiEvaluation.setQuestionId(22L);
//        questionAiEvaluation.setUserId(7L);
//        questionAiEvaluation.setAnswerId(99L);

        when(aiEvaluationService.evaluateInterviewAnswer("Java 答案", "Java并发", "答案")).thenReturn(aiResult);
        when(interviewQuestionInfoMapper.selectOne(any())).thenReturn(questionInfo);
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of());
        when(userQuestionAnswerMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(null);
        when(userQuestionAnswerMapper.insert(any(UserQuestionAnswer.class))).thenAnswer(invocation -> { //invocation 代表：这一次 Mock 方法调用的信息
            UserQuestionAnswer answer = invocation.getArgument(0); //取出这次 insert() 调用的第 0 个参数，也就是 new 的这个 answer，因为 insert()中只有一个参数
            answer.setId(99L);
            return 1;
        });
        when(questionAiEvaluationMapper.selectOne(any())).thenReturn(null); //对应 saveOrUpdateLatestEvaluation(questionAiEvaluation);
        when(questionAiEvaluationMapper.insert(any(QuestionAiEvaluation.class))).thenReturn(1);

        /**
         UserQuestionAnswer answer = new UserQuestionAnswer();
         answer.setQuestionId(22L);
         answer.setUserId(7L);
         answer.setBankId(11L);

         //我说一下，什么时候用这个方法：
         //就是这个参数 answer 必须是 已经存在的情况（例如逻辑删除了），而不能是 新增的
         //为什么？
         //如果新增，你要自己写一个 292行，自己设定一个返回值 answerId = 99L，然后你最后再去验证 assertEquals(99L,answerId)，这完全就是掩耳盗铃了，没有意义，它必然是99L
         //而如果这个 answer 是 existing 的状态，那么你不必写 292行，最后校验assertEquals(99L,answerId)，如果一致，说明saveOrUpdateLatestAnswer() 方法内部一切正常
         //所以对于新增的，只能用 ArgumentCaptor 或者 argThat 去验证
         //如何验证？
         //通过verify(mock对象).方法名(captor.capture()); 既验证方法是否执行，也验证方法的参数 (UserQuestionAnswer对象) 是否被正确传入，被正确传入，说明对象被正确构建了）
         //所以从我的角度看，ArgumentCaptor 反倒是一个无奈之举
         //因为如果我们能通过自定义一个参数，然后通过检验这一整个方法的结果（返回值）就能判定一个方法是否正常运行，就不必大费周章去设置 ArgumentCaptor了
         //但关键条件是：这个返回值不能是你预设的，比如 answerId 不能是 提前预设的
         //这也是为什么新增的 answer 不能使用 ReflectionTestUtils 的原因，因为 answer如果是新增，那么从业务代码的角度，我必须给它预设一个 answerId，作为 saveOrUpdateLatestAnswer()的返回值
         //这是业务逻辑造成的，业务要求，如果 answer 不存在，那么要插入一条新的数据，那么自然要返回 answerId
         //而一旦预设了这个 answerId，  ReflectionTestUtils方法将变得毫无意义，你去 assertEquals 你预设的值，那起不到测试的效果

         Long answerId = ReflectionTestUtils.invokeMethod(service,"saveOrUpdateLatestAnswer",answer);
         assertEquals(99L,answerId);
         */

        InterViewAnswerPageVO result = service.getInterviewAnswer(dto);

        assertEquals(22L, result.getQuestionId());
        assertEquals("答案", result.getAnalysis());
        assertEquals(aiResult, result.getAiResult());
        assertFalse(result.getIsFavorite());
        assertTrue(result.getAiEvaluationEnabled());

        //效果完全等同于下方的 ArgumentCaptor 方法，但是针对 多参数 的情况，ArgumentCaptor的结果反馈更清晰
//        verify(userQuestionAnswerMapper).insert(org.mockito.ArgumentMatchers.<UserQuestionAnswer>argThat(userAnswer ->
//                userAnswer.getQuestionId().equals(22L)
//                        && userAnswer.getUserId().equals(7L)
//                        && userAnswer.getBankId().equals(11L)));
        ArgumentCaptor<UserQuestionAnswer> argumentCaptor = ArgumentCaptor.forClass(UserQuestionAnswer.class);

        //关于 argumentCaptor.capture()
        //不仅验证 inser() 是否发生，还捕获了插入的参数。
        //找到调用 insert() 时传入的真实对象，并把它保存起来。
        //所以用 ArgumentCaptor 的意义，其实主要还是为了验证 insert()方法的参数是否正确
        //只不过这样写，可以每一个字段单独校验pass or fail，效果比 org.mockito.ArgumentMatchers 清晰
        //仅此而已，那些说什么主要作用是为了检查 之前的代码是否被正确执行，都是扯淡，我们应该用结果验证代码，而非过程
        //因此，argumentCaptor.capture() 只是一种比较清晰细致的 验证方法参数的字段的值的方法而已
        verify(userQuestionAnswerMapper).insert(argumentCaptor.capture());
        UserQuestionAnswer actualArgument = argumentCaptor.getValue();

        //当要检查很多字段时，ArgumentCaptor 更清晰，因为每个字段都能单独断言，测试失败时也更容易知道具体是哪个字段错误
        assertEquals(99L, actualArgument.getId());
        assertEquals(22L, actualArgument.getQuestionId());
        assertEquals(7L, actualArgument.getUserId());
        assertEquals(11L, actualArgument.getBankId());
        assertEquals(QuestionInfoQuestionType.ESSAY, actualArgument.getQuestionType());
        assertEquals(BigDecimal.valueOf(90), actualArgument.getAiScoreRate());
        assertNotNull(actualArgument.getAnsweredTime());
        assertEquals("Java并发", actualArgument.getContent());
        verify(userQuestionAnswerMapper, never()).restoreById(anyLong()); // never() 和 any() 要搭配使用
        verify(userQuestionAnswerMapper, never()).overwriteAllUpdate(any(UserQuestionAnswer.class));


        ArgumentCaptor<QuestionAiEvaluation> aiEvaluationArgumentCaptor = ArgumentCaptor.forClass(QuestionAiEvaluation.class);
        verify(questionAiEvaluationMapper).insert(aiEvaluationArgumentCaptor.capture());
        QuestionAiEvaluation actualAiEvaluationArgument = aiEvaluationArgumentCaptor.getValue();

        assertNotNull(actualAiEvaluationArgument);
        assertEquals(22L, actualAiEvaluationArgument.getQuestionId());
        assertEquals(7L, actualAiEvaluationArgument.getUserId());
        assertEquals(BigDecimal.valueOf(90), actualAiEvaluationArgument.getScoreRate());
        assertEquals(99L, actualAiEvaluationArgument.getAnswerId());
        verify(questionAiEvaluationMapper, never()).updateById(any(QuestionAiEvaluation.class));

    }


    @Test
    public void getInterviewQuestionReviewTest() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        InterviewQuestionInfo info = new InterviewQuestionInfo();
        info.setId(22L);
        info.setQuestionType(QuestionInfoQuestionType.ESSAY);
        info.setBankId(11L);
        info.setImageObjectKey("questions/java-concurrency.png");
        info.setAnalysis("答案");
        info.setTitle("题目");
        info.setStatus(QuestionInfoStatus.PUBLISHED);


        UserFavoriteQuestion favoriteQuestion = favorite(88L, false);

        UserQuestionAnswer answer = new UserQuestionAnswer();
        answer.setId(99L);
        answer.setQuestionId(22L);
        answer.setUserId(7L);
        answer.setBankId(11L);
        answer.setContent("Java");
        answer.setQuestionType(QuestionInfoQuestionType.ESSAY);


        QuestionAiEvaluation aiEvaluation = new QuestionAiEvaluation();
        aiEvaluation.setQuestionId(22L);
        aiEvaluation.setUserId(7L);
        aiEvaluation.setScoreRate(BigDecimal.valueOf(90));
        aiEvaluation.setAnswerId(99L);
        aiEvaluation.setSummary("总体表现优秀");
        aiEvaluation.setAccurateComment("回答准确");
        aiEvaluation.setInnovativeComment("有自己的思考");
        aiEvaluation.setMissingComment("可以补充线程池参数");
        aiEvaluation.setWrongComment("没有明显错误");
        aiEvaluation.setModelName("test-model");

        /**
         * 这里你犯了一个错误
         * AiEvaluationResult aiResult = new AiEvaluationResult(); 虽然在 内部私有方法 fetchAiResult() 中，但它并不是一个外部依赖
         * 也就是它不是mapper层的代理对象，它只是一个普通的 AiEvaluationResult对象，程序执行到这里，完全可以自己去 new，不需要你自己在测试中预设
         * 所以你只需要控制源头，预设一个 QuestionAiEvaluation aiEvaluation 就可以了
         */
//        AiEvaluationResult aiResult = new AiEvaluationResult();
//        aiResult.setScoreRate(BigDecimal.valueOf(90));

        when(interviewQuestionInfoMapper.selectList(any())).thenReturn(List.of(info));
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favoriteQuestion));

        //用这一条说一下：Mockito 只是模拟了 userQuestionAnswerMapper 调用 selectList(queryWrapper) 这个方法，因为单元测试不直接使用真实数据库和 mapper层依赖
        //但这不代表这个方法之前的 LambdaQueryWrapper 的内容也是Mock的，相反，它是真实的
        //又因为我们要测试的是正常情况下，也就是 UserQuestionAnswer 中确实有 InterviewQuestionInfo 中的题目，也就是确实有答题记录
        //所以你必须要自己 new 一个 userQuestionAnswer对象，然后成员变量要和 interviewQuestionInfo对象 要对齐
        //这样才能确保 List<UserQuestionAnswer> 查到的确实是我们设置的目标题目（info），也就是确实答了这道题
        //也就是这个 List.of(answer) 虽然是 mock 的，但其实它只是没经过数据库，但不代表它是错误的返回值
        //我们只有拿着正确的返回值，才能保证后续的代码执行都是正确的，测试才有意义
        when(userQuestionAnswerMapper.selectList(any())).thenReturn(List.of(answer));
        when(questionAiEvaluationMapper.selectList(any())).thenReturn(List.of(aiEvaluation));
        when(readUrlSigner.sign("questions/java-concurrency.png"))
                .thenReturn("https://cos.example.com/questions/java-concurrency.png?signed=true");


        List<InterviewQuestionReviewVO> result = service.getInterviewQuestionReview(11L);

        //针对 List 的返回值，要先断言 result.size 不是0，否则如果抛异常IndexOutOfBoundsException，不能清楚说明问题
        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).getQuestionId());
        assertEquals("题目", result.get(0).getTitle());
        assertEquals("https://cos.example.com/questions/java-concurrency.png?signed=true", result.get(0).getImageUrl());
        assertEquals(QuestionInfoQuestionType.ESSAY, result.get(0).getQuestionType());
        assertEquals("答案", result.get(0).getAnalysis());
        assertTrue(result.get(0).getIsFavorite());
        assertTrue(result.get((0)).getIsCorrect()); //你不用管 aiResult.getScoreRate().compareTo(BigDecimal.valueOf(60)) >= 0 的判定结果，这是代码的工作，你要做的是验证结果，除非代码一定要你插手的事务（mock外部依赖），否则一切工作交给代码自动执行
        assertEquals("Java", result.get(0).getContent());

        /**
         * 这里还有一个小技巧
         * 测试的返回值 和 依赖所需要的返回值 不同时（本题中，依赖是questionAiEvaluationMapper，依赖返回是 QuestionAiEvaluation，测试方法返回值是 AiEvaluationResult）
         * 此时因为无法直接使用 assertEquals
         * 需要把方法返回值单独拿出来，如下
         */
//        assertEquals(aiResult,result.get(0).getAiResult());

        /**
         * 你把测试方法返回值单独拿出来，它就可以用 AiEvaluationResult类型的 变量 接住了
         * 然后你针对这个 变量 进行断言就可以了
         */
        AiEvaluationResult aiResult = result.get(0).getAiResult();

        assertEquals(BigDecimal.valueOf(90), aiResult.getScoreRate());
        assertEquals("总体表现优秀", aiResult.getSummary());
        assertEquals("回答准确", aiResult.getAccurateComment());
        assertEquals("有自己的思考", aiResult.getInnovativeComment());
        assertEquals("可以补充线程池参数", aiResult.getMissingComment());

        verify(membershipAccessService).requireActiveMembership(7L);
        verify(publishedQuestionBankAccessService).requirePublished(11L);

        /**
         * 对于InterviewQuestionInfoMapper.selectList(interviewQueryWrapper)，
         * userFavoriteQuestionMapper.selectList(favQuery)，
         * userQuestionAnswerMapper.selectList(userAnswerQueryWrapper)
         * 没有必要专门单独的 verify，因为很多 assertEquals 的结果就已经能够验证 这些方法确实执行了
         * 比如：assertTrue(actual.getIsFavorite())，assertEquals("Java",result.get(0).getContent())
         * 比如：assertEquals(BigDecimal.valueOf(90),aiResult.getScoreRate());
         * 那肯定说明 when(questionAiEvaluationMapper.selectList(any())).thenReturn(List.of(aiEvaluation)); 是正确执行了的，不然不可能有值
         */
    }

    @Test
    void saveUserQuestionNoteTest() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        UserQuestionNoteDTO noteDTO = new UserQuestionNoteDTO();
        noteDTO.setQuestionId(22L);
        noteDTO.setBankId(11L);
        noteDTO.setNoteContent("Java");

        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(certificateQuestionInfoMapper.selectCount(any())).thenReturn(0L);
        when(userQuestionNoteMapper.insert(any(UserQuestionNote.class))).thenReturn(1);

        service.saveUserQuestionNote(noteDTO);

        ArgumentCaptor<UserQuestionNote> captor = ArgumentCaptor.forClass(UserQuestionNote.class);
        verify(userQuestionNoteMapper).insert(captor.capture());
        UserQuestionNote userQuestionNote = captor.getValue();

        assertEquals(22L, userQuestionNote.getQuestionId());
        assertEquals(11L, userQuestionNote.getBankId());
        assertEquals("Java", userQuestionNote.getNoteContent());
    }

    @Test
    void getCertificateByBankIdTest() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        List<String> options = List.of("A", "B", "C", "D");
        CertificateQuestionInfo info = new CertificateQuestionInfo();
        info.setId(22L);
        info.setQuestionType(QuestionInfoQuestionType.ESSAY);
        info.setBankId(11L);
        info.setStatus(QuestionInfoStatus.PUBLISHED);
        info.setOptions(options);
        info.setImageObjectKey("questions/java-concurrency.png");
        info.setTitle("Java");

        UserFavoriteQuestion favoriteQuestion = favorite(88L, false);

        when(certificateQuestionInfoMapper.selectList(any())).thenReturn(List.of(info));
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favoriteQuestion));
        when(readUrlSigner.sign("questions/java-concurrency.png"))
                .thenReturn("https://cos.example.com/questions/java-concurrency.png?signed=true");

        List<CertificateQuestionPageVO> result = service.getCertificateByBankId(11L); //这一步是真实的方法调用和传参

        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).getQuestionId());
        assertEquals(options, result.get(0).getOptions());
        assertTrue(result.get(0).getIsFavorite());
        assertEquals("Java", result.get(0).getTitle());
        assertEquals("https://cos.example.com/questions/java-concurrency.png?signed=true", result.get(0).getImageUrl());
        assertEquals(QuestionInfoQuestionType.ESSAY, result.get(0).getQuestionType());
        verify(certificateQuestionInfoMapper).certificateViewCount(11L);

    }

    @Test
    void getCertificateAnswerTest() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        CertificateQuestionSubmitDTO dto = new CertificateQuestionSubmitDTO();
        dto.setQuestionId(22L);
        dto.setBankId(11L);
        dto.setQuestionType(QuestionInfoQuestionType.SINGLE_CHOICE);
        dto.setChosenOptions(List.of("A"));

        CertificateQuestionInfo info = new CertificateQuestionInfo();
        info.setId(22L);
        info.setBankId(11L);
        info.setStatus(QuestionInfoStatus.PUBLISHED);
        info.setQuestionType(QuestionInfoQuestionType.SINGLE_CHOICE);
        info.setAnalysis("Java");
        info.setCorrectAnswer(List.of("A"));

        UserQuestionAnswer existing = new UserQuestionAnswer();
        existing.setId(99L);
        existing.setQuestionId(22L);
        existing.setUserId(7L);
        existing.setBankId(11L);
        existing.setDeleted(true);

        UserFavoriteQuestion favoriteQuestion = favorite(88L, false);
        when(certificateQuestionInfoMapper.selectOne(any())).thenReturn(info);
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favoriteQuestion));
        //when() 可以相对宽松，用来保证测试场景能够执行完；verify() 使用具体参数，验证业务代码传参是否正确
        //如果 Stub 使用具体参数,一旦参数传错
        //这条 Stub 就无法匹配，Mockito 返回默认值 null。业务代码会错误地进入新增分支，最后可能因为 insert() 返回默认值 0 而抛出 DATA_ERROR
        //如果 Stub 使用宽松匹配，业务方法可以继续执行到最后，然后 verify 会给出更明确的参数不匹配信息
        when(userQuestionAnswerMapper.selectIncludingDeletedForUpdate(anyLong(), anyLong(), anyLong())).thenReturn(existing);
        when(userQuestionAnswerMapper.restoreById(anyLong())).thenReturn(1);
        when(userQuestionAnswerMapper.overwriteAllUpdate(any(UserQuestionAnswer.class))).thenReturn(1);

        /**
         * 你千万注意，ReflectionTestUtils.invokeMethod() 主要用在 单独测试某一个内部方法，特别是 私有方法
         * 比如你想单独开一个单元测试，测试 saveOrUpdateLatestAnswer()，那么可以使用这个方法
         * 但是同样的，你在测试 getCertificateAnswer() 的方法中，不应该单独把它拿出来进行校验
         * 因为我们始终要秉持一个原则：java能自己执行的，真实的代码，我们就不应该单独为其配置mock环境 并且也不应该管它，程序会自动执行的
         * 在一个单元测试中，永远只有 被测试的方法 和 mock环境，一个 @Test 只能有一个被测试的方法，也就是 ACT的部分只能有一个 真实方法，就是被测试的方法
         * 所以在 getCertificateAnswer() 中，saveOrUpdateLatestAnswer() 不应该被单独拿出来测试，java会自动执行它的
         *
         * 至于说你如何保证 非主线测试 的准确性，也就是 本测试方法的主要目的是测返回值 CertificateAnswerPageVO，但还有一个支线任务是 保存 userQuestionAnswer 到 数据库表
         * 首先，测试的准确性是通过 pass or fail 反馈的
         * 其次，始终要秉持 java的真实代码 不能干预 的原则，你只需要确保 mock依赖方法执行了，并且传入的参数是你期望的，这就足够了
         * 还是那句话，普通真实代码的执行靠java，mock依赖方法和参数靠你手动把控，所以你就 verify mock依赖方法的指定参数传入后的执行情况就行
         */
//        Long answerId = ReflectionTestUtils.invokeMethod(service, "saveOrUpdateLatestAnswer", existing);
//        assertEquals(99L, answerId);
//        verify(userQuestionAnswerMapper).restoreById(99L);

        //验证方法的返回值
        CertificateAnswerPageVO result = service.getCertificateAnswer(dto);
        assertEquals(22L, result.getQuestionId());
        assertEquals("Java", result.getAnalysis());
        assertEquals(List.of("A"), result.getCorrectAnswer());
        assertTrue(result.getIsFavorite());
        assertTrue(result.getCorrect());

        //再次说一下这里：
        //因为要验证 userQuestionAnswerMapper.overwriteAllUpdate(userQuestionAnswer)
        //然后我们发现一个问题：这个userQuestionAnswer，不是任何 mock依赖 的返回值，所以你就没办法通过 new 一个 userQuestionAnswer，然后用 thenReturn 返回它
        //那么你能不能直接 new 一个 userQuestionAnswer 呢？ 不论是否有 mock依赖调用方法返回它？
        //答案是 不行
        //因为我们在前面说过，只要是真实的代码，不需要你去搭建 mock环境的，那么就不用你去创建对象
        //事实上，你创建了 userQuestionAnswer 也没用，因为程序是按照 dto 里的参数去自动创建 userQuestionAnswer 然后传到 overwriteAllUpdate() 的
        //所以，就算你 new 了，也不会被执行
        //所以，userQuestionAnswerMapper.overwriteAllUpdate(userQuestionAnswer) 这里，才需要用 捕获，去检测：被传入到参数中的 userQuestionAnswer 的成员变量 的值 是什么
        ArgumentCaptor<UserQuestionAnswer> captor = ArgumentCaptor.forClass(UserQuestionAnswer.class);
        verify(userQuestionAnswerMapper).overwriteAllUpdate(captor.capture());
        UserQuestionAnswer actualArgument = captor.getValue();
        assertEquals(99L, actualArgument.getId()); //在这一步，也校验了 参数中传入的 userQuestionAnswer对象，就是 existing，因为 userQuestionAnswer.id 相同
        assertEquals(22L, actualArgument.getQuestionId());
        assertEquals(7L, actualArgument.getUserId());
        assertEquals(11L, actualArgument.getBankId());
        assertEquals(QuestionInfoQuestionType.SINGLE_CHOICE, actualArgument.getQuestionType());
        assertEquals(List.of("A"), actualArgument.getChosenOptions());
        assertNotNull(actualArgument.getAnsweredTime());
        assertTrue(actualArgument.getIsCorrect());

        verify(userQuestionAnswerMapper).restoreById(99L);
        //这种带有分支的代码，当我们选择测试 != null，那么也要把 == null 的情况给测一下（用 never()）
        verify(userQuestionAnswerMapper, never()).insert(any(UserQuestionAnswer.class));

        verify(membershipAccessService).requireActiveMembership(7L);
        verify(publishedQuestionBankAccessService).requirePublished(11L);
    }

    @Test
    void getCertificateRecordTest() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        /**
         * 一个测试方法测试一个明确的业务场景或行为，当然这个场景里可以有很多个断言
         * 比如：有答案+有AI，有答案+无AI，分数低于60分，都应该单独分开测
         */

        CertificateQuestionInfo info = new CertificateQuestionInfo();
        info.setId(22L);
        info.setQuestionType(QuestionInfoQuestionType.SINGLE_CHOICE);
        info.setBankId(11L);
        info.setImageObjectKey("questions/java-concurrency.png");
        info.setTitle("Java");
        info.setCorrectAnswer(List.of("A"));
        info.setOptions(List.of("A", "B", "C"));
        info.setAnalysis("答案");
        info.setStatus(QuestionInfoStatus.PUBLISHED); //这个条件虽然是筛选条件，不会被 mock 的 mapper真正使用，但为了提高测试的真实性和可读性，还是要写上

        UserFavoriteQuestion favoriteQuestion = favorite(88L, false);

        UserQuestionAnswer answer = new UserQuestionAnswer();
        answer.setId(99L);
        answer.setQuestionId(22L);
        answer.setUserId(7L);
        answer.setBankId(11L);
        answer.setChosenOptions(List.of("A"));
        answer.setIsCorrect(true);
        //虽然 返回值CertificateQuestionReviewVO中 的 questionType 是 questionInfo 的，但是 answer 里还是要写
        //因为真实代码运行到  LambdaQueryWrapper<UserQuestionAnswer> 中也有 questionType 这个查询条件，因此也要用到
        //但是，注意：筛选条件（queryWrapper）对生产代码非常重要，但当前使用 Mockito 的 Service 单元测试不会真实执行这些筛选条件
        //为什么仍建议补充？
        //因为 Mock 返回值应该尽可能模拟真实 Mapper 能返回的数据
        //不是为了让当前代码能够通过，而是为了让测试数据与真实场景保持一致，让阅读测试的人能够理解：这是一条单选题答题记录
        //所以它属于“提高测试真实性和可读性”的修改，而不是“让测试通过的必要修改”
        answer.setQuestionType(QuestionInfoQuestionType.SINGLE_CHOICE);

        when(certificateQuestionInfoMapper.selectList(any())).thenReturn(List.of(info));
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favoriteQuestion));
        when(userQuestionAnswerMapper.selectList(any())).thenReturn(List.of(answer));
        when(readUrlSigner.sign("questions/java-concurrency.png"))
                .thenReturn("https://cos.example.com/questions/java-concurrency.png?signed=true");

        List<CertificateQuestionReviewVO> result = service.getCertificateRecord(11L);
        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).getQuestionId());
        assertEquals("Java", result.get(0).getTitle());
        assertEquals(List.of("A", "B", "C"), result.get(0).getOptions());
        assertEquals("https://cos.example.com/questions/java-concurrency.png?signed=true", result.get(0).getImageUrl());
        assertEquals(List.of("A"), result.get(0).getChosenOptions());
        assertEquals(List.of("A"), result.get(0).getCorrectAnswer());
        assertEquals("答案", result.get(0).getAnalysis());
        assertEquals(QuestionInfoQuestionType.SINGLE_CHOICE, result.get(0).getQuestionType());
        assertTrue(result.get(0).getIsCorrect());
        assertTrue(result.get(0).getIsFavorite());

        verify(membershipAccessService).requireActiveMembership(7L);
        verify(publishedQuestionBankAccessService).requirePublished(11L);

    }

    @Test
    void getInterviewRecordTest() {
        when(membershipAccessService.requireActiveMembership(7L)).thenReturn(membershipAccessSnapshot);

        InterviewQuestionInfo info = new InterviewQuestionInfo();
        info.setId(22L);
        info.setBankId(11L);
        info.setTitle("title");
        info.setImageObjectKey("questions/java-concurrency.png");
        info.setQuestionType(QuestionInfoQuestionType.ESSAY);
        info.setAnalysis("analysis");
        info.setStatus(QuestionInfoStatus.PUBLISHED);

        UserFavoriteQuestion favoriteQuestion = favorite(88L, false);
        QuestionAiEvaluation questionAiEvaluation = new QuestionAiEvaluation();
        questionAiEvaluation.setQuestionId(22L);
        questionAiEvaluation.setScoreRate(BigDecimal.valueOf(90));
        questionAiEvaluation.setAnswerId(99L);
        questionAiEvaluation.setUserId(7L);

        UserQuestionAnswer answer = new UserQuestionAnswer();
        answer.setId(99L);
        answer.setQuestionId(22L);
        answer.setBankId(11L);
        answer.setUserId(7L);
        answer.setQuestionType(QuestionInfoQuestionType.ESSAY);
        answer.setContent("java");

        when(interviewQuestionInfoMapper.selectList(any())).thenReturn(List.of(info));
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favoriteQuestion));
        when(questionAiEvaluationMapper.selectList(any())).thenReturn(List.of(questionAiEvaluation));
        when(userQuestionAnswerMapper.selectList(any())).thenReturn(List.of(answer));
        when(readUrlSigner.sign("questions/java-concurrency.png"))
                .thenReturn("https://cos.example.com/questions/java-concurrency.png?signed=true");


        List<InterviewQuestionReviewVO> result = service.getInterviewRecord(11L);
        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).getQuestionId());
        assertEquals("title", result.get(0).getTitle());
        assertEquals("analysis", result.get(0).getAnalysis());
        assertEquals(QuestionInfoQuestionType.ESSAY, result.get(0).getQuestionType());
        assertEquals("https://cos.example.com/questions/java-concurrency.png?signed=true", result.get(0).getImageUrl());
        assertEquals("java", result.get(0).getContent());
        assertTrue(result.get(0).getIsFavorite());
        assertTrue(result.get(0).getIsCorrect());

        AiEvaluationResult aiResult = result.get(0).getAiResult(); //1.因为存在基础版 premium，所以 aiResult 很可能为 null
        assertNotNull(aiResult); //3.所以要先验证非null
        assertEquals(BigDecimal.valueOf(90), aiResult.getScoreRate()); //2.那么 aiResult.getScoreRate() 就可能NPE

        verify(membershipAccessService).requireActiveMembership(7L);
        verify(publishedQuestionBankAccessService).requirePublished(11L);
    }

    void finishBankTest() {

        when(interviewQuestionInfoMapper.bankCompletionCount(any())).thenReturn(1);
        when(certificateQuestionInfoMapper.bankCompletionCount(any())).thenReturn(1);
        when(certificateQuestionInfoMapper.selectCount(any())).thenReturn(1L);
    }

    @Test
    void buildQuestionCountVOTest() {
        /**
         * 这个测试方法也非常有研究价值
         * 1. 方法体中存在对于两个分支的判断：GroupType.CERTIFICATION 和 GroupType.INTERVIEW
         * 如果我们决定测试的是 GroupType.CERTIFICATION，那么围绕 GroupType.CERTIFICATION 搭建参数和 stub 就可以了，不用写 GroupType.INTERVIEW（事实上你写了也用不到，因为根据你的参数也不会执行）
         * 是否需要验证 Interview 分支没执行？通常不需要，因为 Certification 返回值的断言已经证明认证分支执行了。
         * 如果非常希望强调在执行A分支的时候，B分支不会执行，也可以增加一个 verify（而不是 stub）：verify(interviewQuestionInfoMapper, never()).selectList(any());
         *
         * 2.关于 mapper.insert(),这个方法在生产代码里，一定要写返回值 int result，并判断是否为1，你之前的生产代码中，很多都没写返回值，那么就无法判断插入是否成功
         * 3.关于 到底使用 BigDecimal.valueOf(Value) 还是 new BigDecimal("Value")
         * 如果是生产代码，且是long/int，那么使用 BigDecimal.valueOf(Value)，如果是 double，优先使用 BigDecimal.valueOf(Value)
         * 如果是测试代码，且是 double 类型，使用 new BigDecimal("Value")，可以精确保留小数点后的位数，如果是 long/int，使用 BigDecimal.valueOf(Value)即可
         */


        CertificateQuestionInfo info = new CertificateQuestionInfo();
        info.setId(22L);
        info.setQuestionType(QuestionInfoQuestionType.SINGLE_CHOICE);
        info.setBankId(11L);
        info.setStatus(QuestionInfoStatus.PUBLISHED);

        when(certificateQuestionInfoMapper.selectCount(any())).thenReturn(5L);
        when(certificateQuestionInfoMapper.selectList(any())).thenReturn(List.of(info));

        /**
         * 4.关于stub，这里很有说法：也就是 when()里面的方法中的参数，你确实是可以写 基本数据类型，但一定不能写 引用数据类型
         * 因为实际代码执行过程中，它就不可能调用你自己准备的这个对象，因为引用数据类型都是有地址值的，地址值是一定不会相同的
         * 对于一些自己写的，比如 selectIncludingDeleted()方法，确实是可以输入基本数据类型，这没问题，但通常为了最大化通过环境，也会换成 any()
         *
         * 5.由此引发一个问题，在生产代码中，连续两次使用了同一套方法：userQuestionAnswerMapper.selectCount(userAnswerQueryWrapper) 和 userQuestionAnswerMapper.selectCount(correctAnswerQueryWrapper);
         * 但是作为参数的 queryWrapper 你又没办法自己 new（因为地址值不同），那么如何用同一个方法的不同参数，模拟两个不同的返回值？
         * 答案就是：依旧是忽略 queryWrapper 这个参数，用 any() 代替，这样 stub 就还是使用同一个。然后在 thenReturn() 上做文章，也就是输入多个参数作为返回值
         * 这样就可以用来模拟使用顺序：第一次调用 stub，返回参数1；第二次调用 stub，返回参数2；
         *
         * 更清晰的长期设计：
         * 如果这两个计数查询是稳定且重要的业务概念，可以在 mapper 层自己写自定义名称的方法，这样方法名就不会重复了
         */
        when(userQuestionAnswerMapper.selectCount(any())).thenReturn(3L, 2L);
        when(userBankCorrectRateMapper.insert(any(UserBankCorrectRate.class))).thenReturn(1);

        QuestionCountVO result = service.buildQuestionCountVO(11L, GroupType.CERTIFICATION);
        assertEquals(5L, result.getTotalCount());
        assertEquals(2L, result.getCorrectCount());
        assertEquals(new BigDecimal("0.40"), result.getCorrectRate());
        assertEquals(3L, result.getAnsweredCount());

        verify(publishedQuestionBankAccessService).requirePublished(11L);

        ArgumentCaptor<UserBankCorrectRate> captor = ArgumentCaptor.forClass(UserBankCorrectRate.class);
        verify(userBankCorrectRateMapper).insert(captor.capture());

        UserBankCorrectRate correctRate = captor.getValue();
        assertEquals(7L, correctRate.getUserId());
        assertEquals(11L, correctRate.getBankId());
        assertEquals(GroupType.CERTIFICATION, correctRate.getGroupType());
        assertEquals(new BigDecimal("0.40"), correctRate.getCorrectRate());
    }

    @Test
    void startAiChatTest() {

        reset(membershipAccessService);
        MembershipAccessSnapshot premiumPlusSnapshot = new MembershipAccessSnapshot(MembershipStatus.PREMIUM_PLUS, MembershipType.PREMIUM_PLUS, null, null);

        //这里你有没想明白的点：
        //当 membershipAccessService.requirePremiumPlus 时，你不用去 stub membershipAccessService.requireActiveMembership了啊
        //反正都是在 mock，你就不用管 requirePremiumPlus中是怎么再调用 requireActiveMembership 的了，因为 requirePremiumPlus 的返回值和 requireActiveMembership 相同
        //你直接 stub requirePremiumPlus 就好了
        when(membershipAccessService.requirePremiumPlus(7L)).thenReturn(premiumPlusSnapshot);

        LocalDateTime createdTime = LocalDateTime.of(2026, 10, 10, 10, 0);

        AiChatMessage aiChatMessage = new AiChatMessage();
        aiChatMessage.setSessionId(99L);
        aiChatMessage.setMessageContent("你好，有关于这道题的知识点想深入了解吗？");
        aiChatMessage.setQuestionId(null);
        aiChatMessage.setGroupType(GroupType.CERTIFICATION);
        aiChatMessage.setModelName("rule-welcome");
        aiChatMessage.setId(9L);
        aiChatMessage.setSenderType(AiChatMessageSenderType.AI);
        aiChatMessage.setCreatedTime(createdTime);

        when(aiChatSessionMapper.selectOne(any())).thenReturn(null);
        when(aiChatSessionMapper.insert(any(AiChatSession.class))).thenAnswer(invocation -> {
            AiChatSession session = invocation.getArgument(0);
            session.setId(99L);
            return 1;
        });
        when(aiChatMessageMapper.insert(any(AiChatMessage.class))).thenAnswer(invocation -> {
            AiChatMessage message = invocation.getArgument(0);
            message.setId(9L);
            return 1;
        });

        when(aiChatMessageMapper.selectList(any())).thenReturn(List.of(aiChatMessage));

        AiChatVO result = service.startAiChat(11L, GroupType.CERTIFICATION);

        assertNotNull(result);
        assertEquals(99L, result.getSessionId());
        assertEquals(11L, result.getBankId());

        List<AiChatMessageVO> messages = result.getMessages();
        assertEquals(1, messages.size());
        assertEquals(9L, messages.get(0).getMessageId());
        assertEquals(AiChatMessageSenderType.AI, messages.get(0).getSenderType());
        assertEquals("你好，有关于这道题的知识点想深入了解吗？", messages.get(0).getMessageContent());
        assertEquals(createdTime, messages.get(0).getCreatedTime());

        ArgumentCaptor<AiChatSession> sessionCaptor = ArgumentCaptor.forClass(AiChatSession.class);
        verify(aiChatSessionMapper).insert(sessionCaptor.capture());
        AiChatSession session = sessionCaptor.getValue();

        assertNotNull(session);
        assertEquals(7L, session.getUserId());
        assertEquals(11L, session.getBankId());
        assertEquals(GroupType.CERTIFICATION, session.getGroupType());
        assertEquals(AiChatSessionStatus.ACTIVE, session.getStatus());

        ArgumentCaptor<AiChatMessage> messageCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(aiChatMessageMapper).insert(messageCaptor.capture());
        AiChatMessage message = messageCaptor.getValue();

        assertNotNull(message);
        assertNull(message.getQuestionId());
        assertEquals(99L, message.getSessionId());
        assertEquals(AiChatMessageSenderType.AI, message.getSenderType());
        assertEquals(GroupType.CERTIFICATION, message.getGroupType());
        assertEquals("你好，有关于这道题的知识点想深入了解吗？", message.getMessageContent());
        assertEquals("rule-welcome", message.getModelName());

        verify(aiChatSessionMapper, never()).updateById(any(AiChatSession.class));
        verify(aiChatMessageMapper, never()).delete(any());

        verify(membershipAccessService).requirePremiumPlus(7L);
        verify(publishedQuestionBankAccessService).requirePublished(11L);

    }


    @Test
    void followUpAiTest() {
        MembershipAccessSnapshot premiumPlusSnapshot = new MembershipAccessSnapshot(MembershipStatus.PREMIUM_PLUS, MembershipType.PREMIUM_PLUS, null, null);
        when(membershipAccessService.requirePremiumPlus(7L)).thenReturn(premiumPlusSnapshot);

        LocalDateTime createdTime = LocalDateTime.of(2026, 10, 10, 10, 0);

        AiChatSession session = new AiChatSession();
        session.setId(99L);
        session.setUserId(7L);
        session.setBankId(11L);
        session.setGroupType(GroupType.CERTIFICATION);
        session.setStatus(AiChatSessionStatus.ACTIVE);

        AiChatMessage history = new AiChatMessage();
        history.setId(9L);
        history.setSessionId(99L);
        history.setSenderType(AiChatMessageSenderType.AI);
        history.setGroupType(GroupType.CERTIFICATION);
        history.setMessageContent("你好，有关于这道题的知识点想深入了解吗？");

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setId(10L);
        userMessage.setSessionId(99L);
        userMessage.setSenderType(AiChatMessageSenderType.USER);
        userMessage.setGroupType(GroupType.CERTIFICATION);
        userMessage.setMessageContent("我想了解");
        userMessage.setCreatedTime(createdTime);

        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setId(11L);
        aiMessage.setSessionId(99L);
        aiMessage.setSenderType(AiChatMessageSenderType.AI);
        aiMessage.setGroupType(GroupType.CERTIFICATION);
        aiMessage.setMessageContent("这是 Mock AI 回复。启用 Qwen 后，将根据当前题目、答案解析和用户问题生成回复。");
        aiMessage.setModelName("mock-llm");

        LlmResponse llmResponse = new LlmResponse(
                "这是 Mock AI 回复。启用 Qwen 后，将根据当前题目、答案解析和用户问题生成回复。",
                "mock-llm",
                null,
                null,
                null
        );

        AiFollowUpDTO aiFollowUpDTO = new AiFollowUpDTO();
        aiFollowUpDTO.setQuestionId(22L);
        aiFollowUpDTO.setBankId(11L);
        aiFollowUpDTO.setGroupType(GroupType.CERTIFICATION);
        aiFollowUpDTO.setMessage("我想了解");

        when(aiChatSessionMapper.selectOne(any())).thenReturn(session);

        /**
         * 多次相同方法的调用，stub的返回值，可以依次输入到参数中
         */
        when(aiChatMessageMapper.selectList(any())).thenReturn(List.of(history), List.of(history, userMessage, aiMessage));
        when(aiPromptBuilder.buildQuestionContext(any(AiFollowUpDTO.class))).thenReturn("questionContext");
        when(aiPromptBuilder.buildAiChatPrompt(anyString(), anyList(), anyString())).thenReturn("prompt");
        when(llmClient.chat(anyString())).thenReturn(llmResponse);

        /**
         * 这是新知识点
         * 当你使用了 thenAnswer，既要设置字段，又要返回值，还要重复使用该方法多次的时候，不能像 thenReturn(value1,value2) 那样
         * 而是要写 thenAnswer(invocation -> {AiChatMessage1}).thenAnswer(invocation -> {AiChatMessage2}) [注意是 点，而非 逗号隔开]
         */
        when(aiChatMessageMapper.insert(any(AiChatMessage.class))).thenAnswer(invocation -> {
                    AiChatMessage firstMessage = invocation.getArgument(0);
                    firstMessage.setId(10L);
                    return 1;
                }).thenAnswer(invocation -> {
                    AiChatMessage secondMessage = invocation.getArgument(0);
                    secondMessage.setId(11L);
                    return 1;
                });
        /*
        还有一种写法，可以参考，拓展思路
        AiChatMessageSenderType senderType = message.getSenderType();

        if (senderType == AiChatMessageSenderType.USER) {
            message.setId(10L);
        } else if (senderType == AiChatMessageSenderType.AI) {
            message.setId(11L);
        } else {
            fail("插入消息的 senderType 不能为 null");
        }
         */

        AiChatVO result = service.followUpAi(aiFollowUpDTO);
        assertNotNull(result);
        assertEquals(99L, result.getSessionId());
        assertEquals(11L, result.getBankId());

        List<AiChatMessageVO> messages = result.getMessages();
        assertEquals(3, messages.size());
        assertEquals(10L, messages.get(1).getMessageId());
        assertEquals(AiChatMessageSenderType.USER, messages.get(1).getSenderType());
        assertEquals("我想了解", messages.get(1).getMessageContent());
        assertEquals(createdTime, messages.get(1).getCreatedTime());

        ArgumentCaptor<AiChatMessage> messageCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
        /**
         * 这里正好和 thenAnswer 形成 combo
         * 前面用 条件判断，这里用 times(2)，表示 被验证方法 被执行 2次，那么就会捕获 2个插入的 AiChatMessage对象（因为在生产代码中，aiChatMessageMapper.insert就执行了2次）
         * 然后 验证2个对象的参数
         */
        verify(aiChatMessageMapper, times(2)).insert(messageCaptor.capture());

        /**
         * 这里要用 getAllValues()，而不是 getValue(),接收两次捕获的对象
         */
        List<AiChatMessage> captorMessages = messageCaptor.getAllValues();

        AiChatMessage captorMessageUser = captorMessages.get(0);
        assertEquals(22L, captorMessageUser.getQuestionId());
        assertEquals(99L, captorMessageUser.getSessionId());
        assertEquals(GroupType.CERTIFICATION, captorMessageUser.getGroupType());
        assertEquals(AiChatMessageSenderType.USER, captorMessageUser.getSenderType());
        assertEquals("我想了解", captorMessageUser.getMessageContent());
        assertNull(captorMessageUser.getModelName());
        //下面这两组，在 verify中验证，意义不大，因为这都是你在 verify的 thenAnswer 中自己设置的，你再捕获他们没意义，必然相同
        //但是我们在 thenAnswer 中自己设置，目的是为了在上面 993行，也就是该测试方法的返回值中必须要验证这个字段，然后进行验证，所以993行的验证是必要的
        assertEquals(createdTime, captorMessageUser.getCreatedTime());
        assertEquals(10L, captorMessageUser.getId());

        AiChatMessage captorMessageAi = captorMessages.get(1);
        assertEquals(22L, captorMessageAi.getQuestionId());
        assertEquals(99L, captorMessageAi.getSessionId());
        assertEquals(GroupType.CERTIFICATION, captorMessageAi.getGroupType());
        assertEquals(AiChatMessageSenderType.AI, captorMessageAi.getSenderType());
        assertEquals("这是 Mock AI 回复。启用 Qwen 后，将根据当前题目、答案解析和用户问题生成回复。", captorMessageAi.getMessageContent());
        assertEquals("mock-llm", captorMessageAi.getModelName());
        //同上注释
        assertEquals(createdTime, captorMessageAi.getCreatedTime());
        assertEquals(11L, captorMessageAi.getId());

        verify(aiPromptBuilder).buildAiChatPrompt("questionContext", List.of(history), "我想了解");
        verify(llmClient).chat("prompt");
        verify(membershipAccessService).requirePremiumPlus(7L);
        verify(publishedQuestionBankAccessService).requirePublished(11L);

    }

}
