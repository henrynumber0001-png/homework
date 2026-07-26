package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.BankDataScope;
import com.homework.model.enums.GroupType;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.mapper.QuestionBankQuestionMapper;
import com.homework.web.admin.vo.QuestionDetailVO;
import com.homework.web.admin.vo.QuestionOptionVO;
import com.homework.web.admin.vo.QuestionRowVO;
import com.homework.web.admin.vo.ReferencedBankVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 将两类题目实体统一转换为后台题目视图。 */
@Service
@RequiredArgsConstructor
public class QuestionAssembler {

    private final QuestionBankQuestionMapper relationMapper;
    private final QuestionBankMapper bankMapper;
    private final AdminAccessService accessService;

    public QuestionRowVO toRow(Long bankId, QuestionBankQuestion relation, InterviewQuestionInfo question) {
        QuestionRowVO vo = new QuestionRowVO();
        vo.setId(question.getId());
        vo.setBankId(bankId);
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(question.getImageUrl());
        vo.setReleased(question.getIsReleased());
        vo.setDeleted(question.getDeleted());
        vo.setBankSortOrder(relation.getSortOrder());
        vo.setReferencedBankCount((long) relationMapper
                .selectQuestionRelations(question.getId(), GroupType.INTERVIEW).size());
        vo.setCreatedTime(question.getCreatedTime());
        vo.setUpdatedTime(question.getUpdatedTime());
        vo.setVersion(question.getVersion());
        return vo;
    }

    public QuestionRowVO toRow(Long bankId, QuestionBankQuestion relation, CertificateQuestionInfo question) {
        QuestionRowVO vo = new QuestionRowVO();
        vo.setId(question.getId());
        vo.setBankId(bankId);
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(question.getImageUrl());
        vo.setReleased(question.getIsReleased());
        vo.setDeleted(question.getDeleted());
        vo.setBankSortOrder(relation.getSortOrder());
        vo.setReferencedBankCount((long) relationMapper
                .selectQuestionRelations(question.getId(), GroupType.CERTIFICATION).size());
        vo.setCreatedTime(question.getCreatedTime());
        vo.setUpdatedTime(question.getUpdatedTime());
        vo.setVersion(question.getVersion());
        return vo;
    }

    public QuestionDetailVO toDetail(
            Long bankId,
            GroupType groupType,
            QuestionBankQuestion currentRelation,
            InterviewQuestionInfo question
    ) {
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setBankId(bankId);
        vo.setGroupType(groupType.name());
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(question.getImageUrl());
        vo.setAnalysis(question.getAnalysis());
        vo.setOptions(List.of());
        vo.setCorrectAnswers(List.of());
        vo.setReleased(question.getIsReleased());
        vo.setDeleted(question.getDeleted());
        vo.setBankSortOrder(currentRelation.getSortOrder());
        vo.setVersion(question.getVersion());
        setReferences(vo, question.getId(), groupType);
        return vo;
    }

    public QuestionDetailVO toDetail(
            Long bankId,
            GroupType groupType,
            QuestionBankQuestion currentRelation,
            CertificateQuestionInfo question
    ) {
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setBankId(bankId);
        vo.setGroupType(groupType.name());
        vo.setQuestionType(question.getQuestionType().name());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(question.getImageUrl());
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
        vo.setDeleted(question.getDeleted());
        vo.setBankSortOrder(currentRelation.getSortOrder());
        vo.setVersion(question.getVersion());
        setReferences(vo, question.getId(), groupType);
        return vo;
    }

    public void setReferences(QuestionDetailVO vo, Long questionId, GroupType groupType) {
        List<QuestionBankQuestion> relations = relationMapper.selectQuestionRelations(questionId, groupType);
        boolean canSeeAll = AdminContext.get().getRole() == AdminRole.SUPER_ADMIN
                || AdminContext.get().getBankDataScope() == BankDataScope.ALL_BANKS;
        List<Long> allowedBankIds = canSeeAll
                ? List.of()
                : accessService.listAssignedBankIds(AdminContext.getAdminId());
        List<ReferencedBankVO> visible = new ArrayList<>();
        for (QuestionBankQuestion relation : relations) {
            if (canSeeAll || allowedBankIds.contains(relation.getBankId())) {
                QuestionBank bank = bankMapper.selectIncludingDeleted(relation.getBankId());
                if (bank != null) {
                    ReferencedBankVO bankVO = new ReferencedBankVO();
                    bankVO.setBankId(bank.getId());
                    bankVO.setBankName(bank.getBankName());
                    visible.add(bankVO);
                }
            }
        }
        vo.setReferencedBankCount((long) relations.size());
        vo.setVisibleReferencedBanks(visible);
        vo.setHasHiddenReferences(visible.size() < relations.size());
    }
}
