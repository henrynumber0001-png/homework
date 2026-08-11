package com.homework.web.app.service.impl;

import com.homework.common.storage.CosReadUrlSigner;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.UserQuestionAnswer;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.model.entity.UserFavoriteQuestion;
import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.model.enums.QuestionInfoStatus;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.AiEvaluationService;
import com.homework.web.app.service.LlmClient;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.service.PublishedQuestionBankAccessService;
import com.homework.web.app.vo.InterviewQuestionPageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @BeforeEach
    void setUp() {
        LoginUserHolder.setUserId(7L);
        when(membershipAccessService.requireActiveMembership(7L))
                .thenReturn(new MembershipAccessSnapshot(
                        MembershipStatus.PREMIUM,
                        MembershipType.PREMIUM,
                        null,
                        null
                ));
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.removeUserId();
    }

    @Test
    void activateMissingFavoriteInsertsCurrentUserAndBank() {
        // 变更：关系表已删除，收藏测试直接模拟题目实体归属校验通过。
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(null);
        when(userFavoriteQuestionMapper.insert(any(UserFavoriteQuestion.class))).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        verify(userFavoriteQuestionMapper).insert(org.mockito.ArgumentMatchers.<UserFavoriteQuestion>argThat(favorite ->
                favorite.getUserId().equals(7L)
                        && favorite.getBankId().equals(11L)
                        && favorite.getQuestionId().equals(22L)
                        && favorite.getCollectedTime() != null));
    }

    @Test
    void deactivateMissingFavoriteIsIdempotent() {
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(null);

        service.collect(11L, 22L, ActionStatus.DEACTIVATE);

        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
        verify(userFavoriteQuestionMapper, never()).restoreById(anyLong());
        verify(userFavoriteQuestionMapper, never()).deactivateById(anyLong());
    }

    @Test
    void activateDeletedFavoriteRestoresFavoriteRecordId() {
        UserFavoriteQuestion existing = favorite(99L, true);
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(existing);
        when(userFavoriteQuestionMapper.restoreById(99L)).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        verify(userFavoriteQuestionMapper).restoreById(99L);
        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
    }

    @Test
    void deactivateActiveFavoriteUsesFavoriteRecordId() {
        UserFavoriteQuestion existing = favorite(99L, false);
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(existing);
        when(userFavoriteQuestionMapper.deleteById(99L)).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.DEACTIVATE);

        verify(userFavoriteQuestionMapper).deleteById(99L);
    }

    @Test
    void repeatedActivateOnActiveFavoriteDoesNotWriteAgain() {
        when(interviewQuestionInfoMapper.selectCount(any())).thenReturn(1L);
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L))
                .thenReturn(favorite(99L, false));

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
        verify(userFavoriteQuestionMapper, never()).restoreById(anyLong());
        verify(userFavoriteQuestionMapper, never()).deactivateById(anyLong());
    }

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

        UserQuestionAnswer newAnswer = new UserQuestionAnswer();
        newAnswer.setUserId(7L);
        newAnswer.setBankId(11L);
        newAnswer.setQuestionId(22L);
        newAnswer.setContent("重新作答");

        Long answerId = ReflectionTestUtils.invokeMethod(
                service,
                "saveOrUpdateLatestAnswer",
                newAnswer
        );

        assertEquals(99L, answerId);
        assertEquals(99L, newAnswer.getId());
        verify(userQuestionAnswerMapper).restoreById(99L);
        verify(userQuestionAnswerMapper).overwriteAllUpdate(newAnswer);
        verify(userQuestionAnswerMapper, never()).insert(any(UserQuestionAnswer.class));
    }

    @Test
    void interviewQuestionListUsesFavoriteMap() {
        InterviewQuestionInfo question = new InterviewQuestionInfo();
        question.setId(22L);
        question.setBankId(11L);
        question.setQuestionNo(1);
        question.setTitle("Java 并发");
        question.setImageObjectKey("questions/java-concurrency.png");
        question.setQuestionType(QuestionInfoQuestionType.ESSAY);
        question.setStatus(QuestionInfoStatus.PUBLISHED);
        UserFavoriteQuestion favorite = favorite(99L, false);

        when(interviewQuestionInfoMapper.selectList(any())).thenReturn(List.of(question));
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favorite));
        when(readUrlSigner.sign("questions/java-concurrency.png"))
                .thenReturn("https://cos.example.com/questions/java-concurrency.png?signed=true");

        List<InterviewQuestionPageVO> result = service.getInterviewByBankId(11L);

        assertTrue(result.get(0).getIsFavorite());
        assertEquals(
                "https://cos.example.com/questions/java-concurrency.png?signed=true",
                result.get(0).getImageUrl()
        );
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
}
