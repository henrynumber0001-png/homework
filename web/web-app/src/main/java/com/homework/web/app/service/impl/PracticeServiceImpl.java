package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.enums.EnumUtils;
import com.homework.common.enums.PracticeSessionSessionMode;
import com.homework.common.enums.PracticeSessionStatus;
import com.homework.common.enums.UserQuestionAnswerAnswerType;
import com.homework.web.app.dto.PracticeSessionCreateDTO;
import com.homework.web.app.dto.QuestionAnswerSubmitDTO;
import com.homework.model.entity.PracticeSession;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.entity.QuestionAiEvaluation;
import com.homework.model.entity.QuestionInfo;
import com.homework.model.entity.QuestionOption;
import com.homework.model.entity.UserQuestionAnswer;
import com.homework.model.entity.UserWrongQuestion;
import com.homework.web.app.mapper.PracticeSessionMapper;
import com.homework.web.app.mapper.QuestionBankQuestionMapper;
import com.homework.web.app.mapper.QuestionAiEvaluationMapper;
import com.homework.web.app.mapper.QuestionInfoMapper;
import com.homework.web.app.mapper.QuestionOptionMapper;
import com.homework.web.app.mapper.UserQuestionAnswerMapper;
import com.homework.web.app.mapper.UserWrongQuestionMapper;
import com.homework.web.app.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private final PracticeSessionMapper practiceSessionMapper;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final QuestionAiEvaluationMapper questionAiEvaluationMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final QuestionBankQuestionMapper questionBankQuestionMapper;
    private final QuestionInfoMapper questionInfoMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> createSession(PracticeSessionCreateDTO dto) {
        PracticeSession session = new PracticeSession();
        session.setUserId(dto.getUserId());
        session.setBankId(dto.getBankId());
        session.setSessionMode(EnumUtils.fromValue(PracticeSessionSessionMode.class, dto.getSessionMode()));
        session.setStatus(PracticeSessionStatus.IN_PROGRESS);
        session.setTotalQuestionCount(countBankQuestions(dto.getBankId()));
        session.setAnsweredCount(0);
        session.setCorrectCount(0);
        session.setWrongCount(0);
        session.setScoreRate(BigDecimal.ZERO);
        session.setStartedTime(LocalDateTime.now());
        practiceSessionMapper.insert(session);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("status", session.getStatus().getValue());
        result.put("totalQuestionCount", session.getTotalQuestionCount());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitAnswer(QuestionAnswerSubmitDTO dto) {
        QuestionInfo question = questionInfoMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }
        AnswerCheck check = checkAnswer(dto);

        UserQuestionAnswer answer = userQuestionAnswerMapper.selectOne(new QueryWrapper<UserQuestionAnswer>()
                .eq("session_id", dto.getSessionId())
                .eq("question_id", dto.getQuestionId())
                .eq("user_id", dto.getUserId())
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        boolean insert = answer == null;
        if (insert) {
            answer = new UserQuestionAnswer();
        }
        answer.setUserId(dto.getUserId());
        answer.setSessionId(dto.getSessionId());
        answer.setQuestionId(dto.getQuestionId());
        answer.setAnswerType(EnumUtils.fromValue(UserQuestionAnswerAnswerType.class, dto.getAnswerType()));
        answer.setAnswerText(dto.getAnswerText());
        answer.setSelectedOptionsJson(dto.getSelectedOptionsJson());
        answer.setCorrect(check.correct());
        answer.setAiScoreRate(check.scoreRate());
        answer.setTimeSpentSeconds(dto.getTimeSpentSeconds() == null ? 0 : dto.getTimeSpentSeconds());
        answer.setAnsweredTime(LocalDateTime.now());
        if (insert) {
            userQuestionAnswerMapper.insert(answer);
        } else {
            userQuestionAnswerMapper.updateById(answer);
        }

        QuestionAiEvaluation evaluation = new QuestionAiEvaluation();
        evaluation.setAnswerId(answer.getId());
        evaluation.setQuestionId(dto.getQuestionId());
        evaluation.setUserId(dto.getUserId());
        evaluation.setScoreRate(check.scoreRate());
        evaluation.setAccurateComment(check.correct() ? "答案与参考结果一致。" : "答案与参考结果不一致。");
        evaluation.setInnovativeComment(dto.getAnswerType() != null && dto.getAnswerType() == 1 ? "简答题可继续接入真实 LLM 做深度评价。" : "选择题按标准答案自动判分。");
        evaluation.setMissingComment(check.correct() ? "暂无明显遗漏。" : "建议查看参考答案与解析。");
        evaluation.setWrongComment(check.correct() ? "暂无明显错误。" : "存在关键选项或概念偏差。");
        evaluation.setSummary(check.correct() ? "本题已答对。" : "本题已记录到错题本。");
        evaluation.setModelName("placeholder-evaluator");
        questionAiEvaluationMapper.insert(evaluation);

        if (!check.correct()) {
            upsertWrongQuestion(dto.getUserId(), dto.getQuestionId());
        }
        refreshSessionStats(dto.getSessionId());

        Map<String, Object> result = new HashMap<>();
        result.put("answerId", answer.getId());
        result.put("scoreRate", check.scoreRate());
        result.put("correct", check.correct());
        result.put("evaluationId", evaluation.getId());
        result.put("referenceAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("aiEvaluation", evaluation);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitSession(Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Practice session not found");
        }
        refreshSessionStats(sessionId);
        session = practiceSessionMapper.selectById(sessionId);
        session.setStatus(PracticeSessionStatus.SUBMITTED);
        session.setSubmittedTime(LocalDateTime.now());
        practiceSessionMapper.updateById(session);
        return getSessionResult(sessionId);
    }

    @Override
    public Map<String, Object> getSessionResult(Long sessionId) {
        refreshSessionStats(sessionId);
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        List<Map<String, Object>> answers = userQuestionAnswerMapper.selectMaps(new QueryWrapper<UserQuestionAnswer>()
                .eq("session_id", sessionId)
                .eq("is_deleted", 0)
                .orderByAsc("id"));
        result.put("answers", answers);
        result.put("totalQuestionCount", session == null ? 0 : session.getTotalQuestionCount());
        result.put("answeredCount", session == null ? answers.size() : session.getAnsweredCount());
        result.put("correctCount", session == null ? 0 : session.getCorrectCount());
        result.put("wrongCount", session == null ? 0 : session.getWrongCount());
        result.put("scoreRate", session == null ? BigDecimal.ZERO : session.getScoreRate());
        return result;
    }

    private AnswerCheck checkAnswer(QuestionAnswerSubmitDTO dto) {
        if (dto.getAnswerType() != null && dto.getAnswerType() == 1) {
            boolean hasAnswer = dto.getAnswerText() != null && !dto.getAnswerText().trim().isEmpty();
            BigDecimal score = hasAnswer ? BigDecimal.valueOf(86) : BigDecimal.ZERO;
            return new AnswerCheck(score.compareTo(BigDecimal.valueOf(60)) >= 0, score);
        }
        Set<String> selected = parseSelectedOptions(dto.getSelectedOptionsJson());
        List<QuestionOption> options = questionOptionMapper.selectList(new QueryWrapper<QuestionOption>()
                .eq("question_id", dto.getQuestionId())
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
        boolean correct = matchesCorrectOptions(selected, options);
        return new AnswerCheck(correct, correct ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
    }

    private Set<String> parseSelectedOptions(String selectedOptionsJson) {
        if (selectedOptionsJson == null || selectedOptionsJson.trim().isEmpty()) {
            return Set.of();
        }
        try {
            List<Object> values = objectMapper.readValue(selectedOptionsJson, new TypeReference<>() {});
            return values.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid selected options json", e);
        }
    }

    private boolean matchesCorrectOptions(Set<String> selected, List<QuestionOption> options) {
        if (selected.isEmpty()) {
            return false;
        }
        Set<String> correctIds = new HashSet<>();
        Set<String> correctKeys = new HashSet<>();
        Set<String> correctZeroBasedIndexes = new HashSet<>();
        Set<String> correctOneBasedIndexes = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            QuestionOption option = options.get(i);
            if (Boolean.TRUE.equals(option.getCorrect())) {
                correctIds.add(String.valueOf(option.getId()));
                correctKeys.add(option.getOptionKey());
                correctZeroBasedIndexes.add(String.valueOf(i));
                correctOneBasedIndexes.add(String.valueOf(i + 1));
            }
        }
        Set<String> normalizedSelected = selected.stream()
                .map(value -> value.length() == 1 ? value.toUpperCase() : value)
                .collect(Collectors.toSet());
        return normalizedSelected.equals(correctIds)
                || normalizedSelected.equals(correctKeys)
                || normalizedSelected.equals(correctZeroBasedIndexes)
                || normalizedSelected.equals(correctOneBasedIndexes);
    }

    private void refreshSessionStats(Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        List<UserQuestionAnswer> answers = userQuestionAnswerMapper.selectList(new QueryWrapper<UserQuestionAnswer>()
                .eq("session_id", sessionId)
                .eq("is_deleted", 0));
        int answered = answers.size();
        int correct = (int) answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        int total = session.getTotalQuestionCount() == null || session.getTotalQuestionCount() == 0
                ? Math.max(answered, countBankQuestions(session.getBankId()))
                : session.getTotalQuestionCount();
        session.setTotalQuestionCount(total);
        session.setAnsweredCount(answered);
        session.setCorrectCount(correct);
        session.setWrongCount(Math.max(0, answered - correct));
        session.setScoreRate(total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correct)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        practiceSessionMapper.updateById(session);
    }

    private Integer countBankQuestions(Long bankId) {
        if (bankId == null) {
            return 0;
        }
        return Math.toIntExact(questionBankQuestionMapper.selectCount(new QueryWrapper<QuestionBankQuestion>()
                .eq("bank_id", bankId)
                .eq("is_deleted", 0)));
    }

    private void upsertWrongQuestion(Long userId, Long questionId) {
        UserWrongQuestion existing = userWrongQuestionMapper.selectOne(new QueryWrapper<UserWrongQuestion>()
                .eq("user_id", userId)
                .eq("question_id", questionId)
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        if (existing == null) {
            UserWrongQuestion wrong = new UserWrongQuestion();
            wrong.setUserId(userId);
            wrong.setQuestionId(questionId);
            wrong.setWrongCount(1);
            userWrongQuestionMapper.insert(wrong);
        } else {
            existing.setWrongCount(existing.getWrongCount() + 1);
            userWrongQuestionMapper.updateById(existing);
        }
    }

    private record AnswerCheck(Boolean correct, BigDecimal scoreRate) {
    }
}
