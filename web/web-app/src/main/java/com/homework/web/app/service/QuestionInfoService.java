package com.homework.web.app.service;

import com.homework.model.entity.UserQuestionNote;
import com.homework.web.app.dto.InterviewQuestionSubmitDTO;
import com.homework.web.app.dto.UserQuestionNoteDTO;
import com.homework.web.app.vo.InterViewAnswerPageVO;
import com.homework.web.app.vo.InterviewQuestionPageVO;

import java.util.List;

public interface QuestionInfoService {
    List<InterviewQuestionPageVO> getQuestionsByBankId(Long bankId);

    InterViewAnswerPageVO getAnswer(InterviewQuestionSubmitDTO submitDTO);


    void saveUserQuestionNote(UserQuestionNoteDTO noteDTO);
}
