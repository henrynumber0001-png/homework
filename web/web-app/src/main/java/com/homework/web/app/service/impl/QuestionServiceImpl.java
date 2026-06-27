package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.common.enums.EnumUtils;
import com.homework.common.enums.QuestionInfoDifficulty;
import com.homework.common.enums.QuestionInfoQuestionStatus;
import com.homework.common.enums.QuestionInfoQuestionType;
import com.homework.web.app.dto.QuestionSaveDTO;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.entity.QuestionInfo;
import com.homework.model.entity.QuestionOption;
import com.homework.model.entity.UserFavoriteQuestion;
import com.homework.web.app.mapper.QuestionBankQuestionMapper;
import com.homework.web.app.mapper.QuestionInfoMapper;
import com.homework.web.app.mapper.QuestionOptionMapper;
import com.homework.web.app.mapper.UserFavoriteQuestionMapper;
import com.homework.web.app.service.QuestionService;
import com.homework.web.app.vo.QuestionOptionVO;
import com.homework.web.app.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionBankQuestionMapper questionBankQuestionMapper;
    private final QuestionInfoMapper questionInfoMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final UserFavoriteQuestionMapper userFavoriteQuestionMapper;

    @Override
    public List<QuestionVO> listQuestionsByBank(Long bankId) {
        List<Long> ids = questionBankQuestionMapper.selectMaps(new QueryWrapper<QuestionBankQuestion>()
                        .select("question_id")
                        .eq("bank_id", bankId)
                        .eq("is_deleted", 0)
                        .orderByAsc("priority", "id"))
                .stream().map(row -> ((Number) row.get("question_id")).longValue()).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return questionInfoMapper.selectList(new QueryWrapper<QuestionInfo>().in("id", ids).eq("is_deleted", 0))
                .stream().map(this::toVO).toList();
    }

    @Override
    public QuestionVO getQuestion(Long questionId) {
        QuestionInfo question = questionInfoMapper.selectById(questionId);
        return question == null ? null : toVO(question);
    }

    @Override
    public Long saveQuestion(QuestionSaveDTO dto) {
        QuestionInfo entity = dto.getId() == null ? new QuestionInfo() : questionInfoMapper.selectById(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setAnswer(dto.getAnswer());
        entity.setAnalysis(dto.getAnalysis());
        entity.setQuestionType(EnumUtils.fromValue(QuestionInfoQuestionType.class, dto.getQuestionType()));
        entity.setDifficulty(EnumUtils.fromValue(QuestionInfoDifficulty.class, dto.getDifficulty()));
        entity.setPremium(dto.getPremium());
        entity.setQuestionStatus(EnumUtils.fromValue(QuestionInfoQuestionStatus.class, dto.getQuestionStatus()));
        entity.setCreateUserId(dto.getCreateUserId());
        if (dto.getId() == null) {
            questionInfoMapper.insert(entity);
        } else {
            questionInfoMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void deleteQuestion(Long id) {
        questionInfoMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> favoriteQuestion(Long userId, Long questionId) {
        UserFavoriteQuestion existing = userFavoriteQuestionMapper.selectOne(new QueryWrapper<UserFavoriteQuestion>()
                .eq("user_id", userId)
                .eq("question_id", questionId)
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        boolean active = existing == null;
        if (active) {
            UserFavoriteQuestion favorite = new UserFavoriteQuestion();
            favorite.setUserId(userId);
            favorite.setQuestionId(questionId);
            userFavoriteQuestionMapper.insert(favorite);
        } else {
            userFavoriteQuestionMapper.deleteById(existing.getId());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("active", active);
        return result;
    }

    private QuestionVO toVO(QuestionInfo entity) {
        QuestionVO vo = new QuestionVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setAnswer(entity.getAnswer());
        vo.setAnalysis(entity.getAnalysis());
        vo.setQuestionType(entity.getQuestionType() == null ? null : entity.getQuestionType().getValue());
        vo.setDifficulty(entity.getDifficulty() == null ? null : entity.getDifficulty().getValue());
        vo.setPremium(entity.getPremium());
        List<QuestionOptionVO> options = questionOptionMapper.selectList(new QueryWrapper<QuestionOption>()
                        .eq("question_id", entity.getId())
                        .eq("is_deleted", 0)
                        .orderByAsc("sort_order", "id"))
                .stream().map(option -> {
                    QuestionOptionVO optionVO = new QuestionOptionVO();
                    optionVO.setId(option.getId());
                    optionVO.setOptionKey(option.getOptionKey());
                    optionVO.setOptionContent(option.getOptionContent());
                    optionVO.setCorrect(option.getCorrect());
                    return optionVO;
                }).toList();
        vo.setOptions(options);
        return vo;
    }
}
