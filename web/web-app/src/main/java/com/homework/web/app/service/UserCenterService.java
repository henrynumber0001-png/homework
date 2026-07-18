package com.homework.web.app.service;

import com.homework.common.result.PageResult;
import com.homework.model.enums.GroupType;
import com.homework.web.app.vo.UserCenterPageVO;
import com.homework.web.app.vo.WrongQuestionBankVO;
import com.homework.web.app.vo.WrongQuestionVO;

public interface UserCenterService {
    UserCenterPageVO getCenterPageInfo(Long userId);

    PageResult<WrongQuestionBankVO> getWrongQuestionBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize);

    PageResult<WrongQuestionVO> getWrongQuestions(Long userId,Long bankId,Integer pageNum, Integer pageSize);
}
