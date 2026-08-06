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

    /** 题库归属和序号都直接取自面试题实体。 */
    public QuestionRowVO toRow(InterviewQuestionInfo question) {
        QuestionRowVO vo = new QuestionRowVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setQuestionType(question.getQuestionType());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setStatus(question.getStatus());
        vo.setQuestionNo(question.getQuestionNo());
        vo.setCreatedTime(question.getCreatedTime());
        vo.setUpdatedTime(question.getUpdatedTime());
        vo.setVersion(question.getVersion());
        return vo;
    }

    /** 题库归属和序号都直接取自认证题实体。 */
    public QuestionRowVO toRow(CertificateQuestionInfo question) {
        QuestionRowVO vo = new QuestionRowVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setQuestionType(question.getQuestionType());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setStatus(question.getStatus());
        vo.setQuestionNo(question.getQuestionNo());
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
        vo.setGroupType(groupType);
        vo.setQuestionType(question.getQuestionType());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setAnalysis(question.getAnalysis());
        vo.setOptions(List.of());
        vo.setCorrectAnswerKeys(List.of());
        vo.setStatus(question.getStatus());
        vo.setQuestionNo(question.getQuestionNo());
        vo.setVersion(question.getVersion());
        return vo;
    }

    /** 认证题详情直接使用实体中的 bankId 和 questionNo。 */
    public QuestionDetailVO toDetail(GroupType groupType, CertificateQuestionInfo question) {
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setGroupType(groupType);
        vo.setQuestionType(question.getQuestionType());
        vo.setTitle(question.getTitle());
        vo.setImageUrl(readUrlSigner.sign(question.getImageObjectKey()));
        vo.setAnalysis(question.getAnalysis());
        List<QuestionOptionVO> optionVos = new ArrayList<>();
        List<String> correctAnswerKeys = new ArrayList<>();

        //这里是把选项内容再“序列化”为选项（通过列表顺序重新组装上Key）
        List<String> optionContents = question.getOptions() == null ? List.of() : question.getOptions();
        for (int index = 0; index < optionContents.size(); index++) {
            QuestionOptionVO option = new QuestionOptionVO();

            //app端，后端也返回optionContents，但不需要组装key。前端直接根据列表的顺序，通过下标生成选项：String.fromCharCode(65 + index)
            //admin端，就还需要单独设置一个key字段，给选项还原回来。因为管理员可能需要修改正确选项，而admin的后端 correctAnswer 的设计逻辑是记录 key，而非content的
            //为什么admin端不像app端一样，也在前端设置为收集optionContent,而非key呢？
            //这是因为admin端是给管理员使用的，不论是创建单个题目，通过点击设置正确答案的选项，还是通过excel批量上传题目，设置key远比写correctContent更方便，因为content可能会很长，还可能写错或写漏

            String key = String.valueOf((char) ('A' + index));
            option.setKey(key);
            option.setContent(optionContents.get(index));
            optionVos.add(option);
            if (question.getCorrectAnswer() != null && question.getCorrectAnswer().contains(optionContents.get(index))) {
                correctAnswerKeys.add(key);
            }
        }
        vo.setOptions(optionVos);
        vo.setCorrectAnswerKeys(correctAnswerKeys);
        vo.setStatus(question.getStatus());
        vo.setQuestionNo(question.getQuestionNo());
        vo.setVersion(question.getVersion());
        return vo;
    }
}
