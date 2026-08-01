package com.homework.web.admin.service;

import com.homework.common.storage.CosReadUrlSigner;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.enums.GroupType;
import com.homework.web.admin.vo.QuestionDetailVO;
import com.homework.web.admin.vo.QuestionOptionVO;
import com.homework.web.admin.vo.QuestionRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 将两类题目实体统一转换为后台题目视图。 */
@Service
@RequiredArgsConstructor
public class QuestionAssembler {

    private final CosReadUrlSigner readUrlSigner;

    /** 变更：原来需要关系实体提供 bankId 和顺序，现在全部直接取自面试题实体。 */
    public QuestionRowVO toRow(InterviewQuestionInfo question) {
        QuestionRowVO vo = new QuestionRowVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setReleased(question.getIsReleased());
        vo.setSortOrder(question.getSortOrder());
        vo.setCreatedTime(question.getCreatedTime());
        vo.setUpdatedTime(question.getUpdatedTime());
        vo.setVersion(question.getVersion());
        return vo;
    }

    /** 变更：原来需要关系实体提供 bankId 和顺序，现在全部直接取自认证题实体。 */
    public QuestionRowVO toRow(CertificateQuestionInfo question) {
        QuestionRowVO vo = new QuestionRowVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setReleased(question.getIsReleased());
        vo.setSortOrder(question.getSortOrder());
        vo.setCreatedTime(question.getCreatedTime());
        vo.setUpdatedTime(question.getUpdatedTime());
        vo.setVersion(question.getVersion());
        return vo;
    }

    /** 变更：一题只属于一个题库，详情不再组装共享题库列表。 */
    public QuestionDetailVO toDetail(GroupType groupType, InterviewQuestionInfo question) {
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setGroupType(groupType.name());
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setAnalysis(question.getAnalysis());
        vo.setOptions(List.of());
        vo.setCorrectAnswers(List.of());
        vo.setReleased(question.getIsReleased());
        vo.setSortOrder(question.getSortOrder());
        vo.setVersion(question.getVersion());
        return vo;
    }

    /** 变更：认证题详情直接使用实体中的 bankId 和 sortOrder，不再查询关系表。 */
    public QuestionDetailVO toDetail(GroupType groupType, CertificateQuestionInfo question) {
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setGroupType(groupType.name());
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setAnalysis(question.getAnalysis());
        List<QuestionOptionVO> optionVos = new ArrayList<>();
        List<String> correctKeys = new ArrayList<>();
        List<String> optionContents = question.getOptions() == null ? List.of() : question.getOptions();
        for (int index = 0; index < optionContents.size(); index++) {
            QuestionOptionVO option = new QuestionOptionVO();
            String key = String.valueOf((char) ('A' + index));
            option.setKey(key);
            option.setContent(optionContents.get(index));
            optionVos.add(option);
            if (question.getCorrectAnswer() != null && question.getCorrectAnswer().contains(optionContents.get(index))) {
                correctKeys.add(key);
            }
        }
        vo.setOptions(optionVos);
        vo.setCorrectAnswers(correctKeys);
        vo.setReleased(question.getIsReleased());
        vo.setSortOrder(question.getSortOrder());
        vo.setVersion(question.getVersion());
        return vo;
    }
}
