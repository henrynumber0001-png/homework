package com.homework.web.app.service;

import com.homework.web.app.vo.MentionUserVO;
import com.homework.web.app.vo.PublicUserProfileActivityVO;
import com.homework.web.app.vo.PublicUserProfileVO;

import java.util.List;

public interface PublicUserProfileService {
    PublicUserProfileVO getProfile(Long currentUserId, Long profileUserId);
    List<PublicUserProfileActivityVO> listActivities(Long currentUserId, Long profileUserId,
                                                    String tab, Integer pageNum, Integer pageSize);
    List<MentionUserVO> searchUsers(Long currentUserId, String keyword, Integer limit);
}
