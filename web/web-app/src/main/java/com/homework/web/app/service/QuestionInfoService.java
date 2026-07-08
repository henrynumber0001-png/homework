package com.homework.web.app.service;

import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.app.dto.AiFollowUpDTO;
import com.homework.web.app.dto.CertificateQuestionSubmitDTO;
import com.homework.web.app.dto.InterviewQuestionSubmitDTO;
import com.homework.web.app.dto.UserQuestionNoteDTO;
import com.homework.web.app.vo.*;

import java.util.List;

public interface QuestionInfoService {
    List<InterviewQuestionPageVO> getQuestionsByBankId(Long bankId);

    InterViewAnswerPageVO getAnswer(InterviewQuestionSubmitDTO submitDTO);


    void saveUserQuestionNote(UserQuestionNoteDTO noteDTO);

    List<CertificateQuestionPageVO> getCertificateByBankId(Long bankId);

    CertificateAnswerPageVO getCertificateAnswer(CertificateQuestionSubmitDTO submitDTO);

    QuestionCountVO finishBank(Long bankId, GroupType groupType);

    /**
     * 获取当前用户在某个题库下的 AI 追问历史。
     * 前端打开“追问AI”弹窗时先调这个方法，用来恢复原会话。
     */
    AiChatVO startAiChat(Long bankId, GroupType bankType);

    /**
     * 在答案解析模块中继续追问 AI。
     * 这个方法会保存用户问题、构造题目上下文、调用 AI、保存 AI 回复，并返回完整会话。
     */
    AiChatVO followUpAi(AiFollowUpDTO dto);

    void closeAiChat(Long bankId);
}
