package com.homework.web.app.service;

import com.homework.model.enums.ActionStatus;
import com.homework.web.app.vo.FollowStateVO;

public interface FollowService {
    FollowStateVO follow(Long currentUserId, Long targetUserId, ActionStatus status);
}
