package com.homework.web.app.service;

import com.homework.web.app.vo.FollowStateVO;

public interface FollowService {
    FollowStateVO follow(Long currentUserId, Long targetUserId, Boolean active);
}
