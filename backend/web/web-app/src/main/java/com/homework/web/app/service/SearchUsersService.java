package com.homework.web.app.service;

import com.homework.web.app.vo.MentionUserVO;

import java.util.List;

public interface SearchUsersService {
    List<MentionUserVO> searchUsers(Long currentUserId, String keyword, Integer limit);
}
