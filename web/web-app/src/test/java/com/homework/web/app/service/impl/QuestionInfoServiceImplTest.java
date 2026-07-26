package com.homework.web.app.service.impl;

import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.entity.UserFavoriteQuestion;
import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.AiEvaluationService;
import com.homework.web.app.service.LlmClient;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.service.PublishedQuestionBankAccessService;
import com.homework.web.app.service.QuestionBankOrderService;
import com.homework.web.app.vo.InterviewQuestionPageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionInfoServiceImplTest {

    @Mock
    private InterviewQuestionInfoMapper interviewQuestionInfoMapper;
    @Mock
    private QuestionBankQuestionMapper questionBankQuestionMapper;
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
    private MembershipAccessService membershipAccessService;
    @Mock
    private PublishedQuestionBankAccessService publishedQuestionBankAccessService;
    @Spy
    private QuestionBankOrderService questionBankOrderService = new QuestionBankOrderService();

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
        when(questionBankQuestionMapper.selectOne(any())).thenReturn(new QuestionBankQuestion());
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
        when(questionBankQuestionMapper.selectOne(any())).thenReturn(new QuestionBankQuestion());
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(null);

        service.collect(11L, 22L, ActionStatus.DEACTIVATE);

        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
        verify(userFavoriteQuestionMapper, never()).restoreById(anyLong());
        verify(userFavoriteQuestionMapper, never()).deactivateById(anyLong());
    }

    @Test
    void activateDeletedFavoriteRestoresFavoriteRecordId() {
        UserFavoriteQuestion existing = favorite(99L, true);
        when(questionBankQuestionMapper.selectOne(any())).thenReturn(new QuestionBankQuestion());
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(existing);
        when(userFavoriteQuestionMapper.restoreById(99L)).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        verify(userFavoriteQuestionMapper).restoreById(99L);
        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
    }

    @Test
    void deactivateActiveFavoriteUsesFavoriteRecordId() {
        UserFavoriteQuestion existing = favorite(99L, false);
        when(questionBankQuestionMapper.selectOne(any())).thenReturn(new QuestionBankQuestion());
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L)).thenReturn(existing);
        when(userFavoriteQuestionMapper.deleteById(99L)).thenReturn(1);

        service.collect(11L, 22L, ActionStatus.DEACTIVATE);

        verify(userFavoriteQuestionMapper).deleteById(99L);
    }

    @Test
    void repeatedActivateOnActiveFavoriteDoesNotWriteAgain() {
        when(questionBankQuestionMapper.selectOne(any())).thenReturn(new QuestionBankQuestion());
        when(userFavoriteQuestionMapper.selectIncludingDeletedForUpdate(7L, 11L, 22L))
                .thenReturn(favorite(99L, false));

        service.collect(11L, 22L, ActionStatus.ACTIVATE);

        verify(userFavoriteQuestionMapper, never()).insert(any(UserFavoriteQuestion.class));
        verify(userFavoriteQuestionMapper, never()).restoreById(anyLong());
        verify(userFavoriteQuestionMapper, never()).deactivateById(anyLong());
    }

    @Test
    void interviewQuestionListUsesFavoriteMap() {
        QuestionBankQuestion bankQuestion = new QuestionBankQuestion();
        bankQuestion.setQuestionId(22L);
        InterviewQuestionInfo question = new InterviewQuestionInfo();
        question.setId(22L);
        question.setTitle("Java 并发");
        question.setQuestionType(QuestionInfoQuestionType.ESSAY);
        question.setIsReleased(true);
        UserFavoriteQuestion favorite = favorite(99L, false);

        when(questionBankQuestionMapper.selectList(any())).thenReturn(List.of(bankQuestion));
        when(interviewQuestionInfoMapper.selectList(any())).thenReturn(List.of(question));
        when(userFavoriteQuestionMapper.selectList(any())).thenReturn(List.of(favorite));

        List<InterviewQuestionPageVO> result = service.getQuestionsByBankId(11L);

        assertTrue(result.get(0).getIsFavorite());
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
