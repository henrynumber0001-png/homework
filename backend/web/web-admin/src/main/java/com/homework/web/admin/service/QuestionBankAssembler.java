package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BankTag;
import com.homework.model.entity.CategoryGroup;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import com.homework.web.admin.mapper.BankTagMapper;
import com.homework.web.admin.mapper.CategoryGroupMapper;
import com.homework.web.admin.mapper.CategoryModuleMapper;
import com.homework.web.admin.mapper.CategorySubModuleMapper;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.vo.NamedIdVO;
import com.homework.web.admin.vo.QuestionBankRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 将题库实体组装成管理端题库 VO。 */
@Service
@RequiredArgsConstructor
public class QuestionBankAssembler {

    private final CategorySubModuleMapper subModuleMapper;
    private final CategoryModuleMapper moduleMapper;
    private final CategoryGroupMapper groupMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final CertificateQuestionMapper certificateQuestionMapper;
    private final BankTagMapper bankTagMapper;

    public QuestionBankRowVO toRow(QuestionBank bank) {
        CategorySubModule subModule = subModuleMapper.selectById(bank.getSubModuleId());
        if (subModule == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }
        CategoryModule module = moduleMapper.selectById(subModule.getModuleId());
        if(module == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }
        CategoryGroup group = groupMapper.selectById(module.getGroupId());
        if (group == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        // 变更：题目总数原来由关系表计算，现在按题目实体的 bank_id 直接统计。
        long questionCount;
        long releasedCount;
        if (group.getGroupType() == GroupType.INTERVIEW) {
            questionCount = interviewQuestionMapper.selectCount(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bank.getId())
            );
            releasedCount = interviewQuestionMapper.selectCount(
                    new LambdaQueryWrapper<InterviewQuestionInfo>()
                            .eq(InterviewQuestionInfo::getBankId, bank.getId())
                            .eq(InterviewQuestionInfo::getIsReleased, true));
        } else {
            questionCount = certificateQuestionMapper.selectCount(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bank.getId())
            );
            releasedCount = certificateQuestionMapper.selectCount(
                    new LambdaQueryWrapper<CertificateQuestionInfo>()
                            .eq(CertificateQuestionInfo::getBankId, bank.getId())
                            .eq(CertificateQuestionInfo::getIsReleased, true));
        }

        QuestionBankRowVO vo = new QuestionBankRowVO();
        vo.setId(bank.getId());
        vo.setBankName(bank.getBankName());
        vo.setGroupType(group.getGroupType());
        NamedIdVO groupVO = new NamedIdVO();
        groupVO.setId(group.getId());
        groupVO.setName(group.getGroupName());
        vo.setGroup(groupVO);
        NamedIdVO moduleVO = new NamedIdVO();
        moduleVO.setId(module.getId());
        moduleVO.setName(module.getModuleName());
        vo.setModule(moduleVO);
        NamedIdVO subModuleVO = new NamedIdVO();
        subModuleVO.setId(subModule.getId());
        subModuleVO.setName(subModule.getSubModuleName());
        vo.setSubModule(subModuleVO);
        vo.setStatus(bank.getStatus());
        vo.setTags(bankTagMapper.selectList(new LambdaQueryWrapper<BankTag>()
                        .eq(BankTag::getBankId, bank.getId())
                        .orderByAsc(BankTag::getId))
                .stream()
                .map(BankTag::getTagName)
                .toList());
        // 变更：原 priority 字段已改为 sortOrder 人工曝光权重。
        vo.setSortOrder(bank.getSortOrder());
        vo.setQuestionCount(questionCount);
        vo.setReleasedQuestionCount(releasedCount);
        vo.setViewCount(bank.getViewCount());
        vo.setCompleteCount(bank.getCompleteCount());
        vo.setPublishedTime(bank.getPublishedTime());
        vo.setUpdatedTime(bank.getUpdatedTime());
        vo.setVersion(bank.getVersion());
        return vo;
    }

}
