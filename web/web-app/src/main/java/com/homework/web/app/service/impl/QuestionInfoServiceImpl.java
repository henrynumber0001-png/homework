package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.*;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.AiEvaluationResult;
import com.homework.web.app.dto.InterviewQuestionSubmitDTO;
import com.homework.web.app.dto.UserQuestionNoteDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.AiEvaluationService;
import com.homework.web.app.service.QuestionInfoService;
import com.homework.web.app.vo.InterViewAnswerPageVO;
import com.homework.web.app.vo.InterviewQuestionPageVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Service
public class QuestionInfoServiceImpl implements QuestionInfoService {
    private final QuestionInfoMapper questionInfoMapper;
    private final QuestionBankQuestionMapper questionBankQuestionMapper;
    private final UserQuestionAnswerMapper userQuestionAnswerMapper;
    private final AiEvaluationService aiEvaluationService;
    private final QuestionAiEvaluationMapper questionAiEvaluationMapper;
    private final UserQuestionNoteMapper userQuestionNoteMapper;

    @Override
    public List<InterviewQuestionPageVO> getQuestionsByBankId(Long bankId) {

        if(bankId == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<QuestionBankQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBankQuestion::getBankId, bankId);

        List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionMapper.selectList(queryWrapper);
        if(questionBankQuestions.isEmpty()){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<Long> questionIds = questionBankQuestions.stream().map(QuestionBankQuestion::getQuestionId).toList();


        LambdaQueryWrapper<QuestionInfo> questionInfoQueryWrapper = new LambdaQueryWrapper<>();
        questionInfoQueryWrapper.in(QuestionInfo::getId,questionIds)
                .eq(QuestionInfo::getQuestionType,QuestionInfoQuestionType.ESSAY)
                .eq(QuestionInfo::getIsReleased,true)
                .orderByAsc(QuestionInfo::getSortOrder) //用sort_order字段，在查询时候给题目一个排序，就能保持每次顺序的固定了
                .orderByAsc(QuestionInfo::getId);

        List<QuestionInfo> questionInfos = questionInfoMapper.selectList(questionInfoQueryWrapper);

        if(questionInfos.isEmpty()){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        List<InterviewQuestionPageVO> list = new ArrayList<>();
        questionInfos.forEach(questionInfo -> {
            InterviewQuestionPageVO vo = new InterviewQuestionPageVO();
            vo.setQuestionId(questionInfo.getId());
            vo.setTitle(questionInfo.getTitle());
            vo.setQuestionType(questionInfo.getQuestionType());
            vo.setBankId(questionInfo.getBankId());
            vo.setReleased(questionInfo.getIsReleased());
            vo.setSortOder(questionInfo.getSortOrder());
            list.add(vo);
        });

        return list;
    }

    @Transactional
    @Override
    public InterViewAnswerPageVO getAnswer(InterviewQuestionSubmitDTO submitDTO) {
        if(submitDTO == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if(!StringUtils.hasText(submitDTO.getContent()) || submitDTO.getQuestionId() == null || submitDTO.getTimeSpentSeconds() == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long questionId = submitDTO.getQuestionId();

        QuestionInfo questionInfo = questionInfoMapper.selectById(questionId);
        if(questionInfo == null){
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        //兜底机制（但后端接口不能只相信前端，因为用户可以绕过页面，直接请求）
        if(!questionInfo.getQuestionType().equals(QuestionInfoQuestionType.ESSAY) || !Boolean.TRUE.equals(questionInfo.getIsReleased())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String title = questionInfo.getTitle();
        String analysis = questionInfo.getAnalysis();
        String content = submitDTO.getContent();

        //提供 题目名称、用户回答内容、题目解析给 AI模型，然后获取 AI模型的评价结果
        AiEvaluationResult aiResult = aiEvaluationService.evaluateInterviewAnswer(title,content,analysis);

        //返回给前端
        InterViewAnswerPageVO answer = new InterViewAnswerPageVO();
        answer.setQuestionId(questionId);
        answer.setAnalysis(questionInfo.getAnalysis());
        answer.setAiResult(aiResult);

        //把用户输入的回答放到 用户ID下的专门的一张表 user_question_answer, 用于用户其他信息查询功能（如答题历史、收藏、错题）
        //后端先保存 UserQuestionAnswer，拿到 answerId
        Long userId = LoginUserHolder.getUserId();
        UserQuestionAnswer userAnswer = new UserQuestionAnswer();
        userAnswer.setUserId(userId);
        userAnswer.setQuestionId(questionId);
        userAnswer.setContent(submitDTO.getContent());
        userAnswer.setQuestionType(QuestionInfoQuestionType.ESSAY);
        userAnswer.setAiScoreRate(aiResult.getScoreRate());
        userAnswer.setTimeSpentSeconds(submitDTO.getTimeSpentSeconds());//用户在这道题上花费了多少秒，这个数据用于后续一系列统计功能的开发
        userAnswer.setAnsweredTime(LocalDateTime.now());

        //insert之后，就会生成answerId;
        userQuestionAnswerMapper.insert(userAnswer);
        //UserQuestionAnswer 负责记录：用户每次提交的答案
        //QuestionAiEvaluation 负责记录：AI 对这个用户这次答案的评价

        //记录一次 AI 对这次用户提交答案的评价
        QuestionAiEvaluation questionAiEvaluation = new QuestionAiEvaluation();
        questionAiEvaluation.setUserId(userId);
        questionAiEvaluation.setQuestionId(questionId);
        questionAiEvaluation.setAnswerId(userAnswer.getId());
        BeanUtils.copyProperties(aiResult,questionAiEvaluation);
        questionAiEvaluationMapper.insert(questionAiEvaluation);

        return answer;
    }

    @Override
    public void saveUserQuestionNote(UserQuestionNoteDTO noteDTO) {
        if(noteDTO == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //用户在笔记里输入 "  "空格字符，是被允许的，null和""空字符串（长度为0）的不行
        if(noteDTO.getBankId() == null || noteDTO.getQuestionId() == null || noteDTO.getNoteContent() == null ||noteDTO.getNoteContent().isEmpty()){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        Long userId = LoginUserHolder.getUserId();
        UserQuestionNote userQuestionNote = new UserQuestionNote();
        userQuestionNote.setUserId(userId);
        userQuestionNote.setBankId(noteDTO.getBankId());
        userQuestionNote.setQuestionId(noteDTO.getQuestionId()); //空格字符串可以保存，但null和空字符串不行
        userQuestionNote.setNoteContent(noteDTO.getNoteContent());
        userQuestionNoteMapper.insert(userQuestionNote);


    }


}
