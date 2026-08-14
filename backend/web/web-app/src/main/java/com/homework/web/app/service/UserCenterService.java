package com.homework.web.app.service;

import com.homework.common.result.PageResult;
import com.homework.model.enums.GroupType;
import com.homework.web.app.dto.EditProfileDTO;
import com.homework.web.app.vo.*;
import jakarta.validation.Valid;

public interface UserCenterService {
    UserCenterPageVO getCenterPageInfo(Long userId);

    PageResult<WrongQuestionBankVO> getWrongQuestionBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize);

    PageResult<WrongQuestionVO> getWrongQuestions(Long userId,Long bankId,Integer pageNum, Integer pageSize);

    WrongQuestionReviewVO getWrongQuestion(Long userId, Long bankId,Long questionId);

    PageResult<FavoriteQuestionBankVO> getFavoriteQuestionBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize);

    PageResult<FavoriteQuestionVO> getFavoriteQuestions(Long userId, Long bankId, Integer pageNum, Integer pageSize);

    FavoriteQuestionReviewVO getFavoriteQuestion(Long userId, Long bankId, Long questionId);

    PageResult<NoteBankVO> getNoteBanks(Long userId, GroupType groupType, Integer pageNum, Integer pageSize);

    PageResult<NoteQuestionVO> getNoteQuestions(Long userId, Long bankId, Integer pageNum, Integer pageSize);

    NoteVO getNote(Long userId, Long bankId, Long questionId);

    MembershipInfoVO getMembershipInfo(Long userId);

    EditedProfileVO editProfile(Long userId, @Valid EditProfileDTO dto);

    ProfileVO getProfile(Long userId);

    ProfileOptionsVO getProfileOptions();

}
