package com.homework.web.app.service;

import com.homework.web.app.vo.UserCenterPageVO;

public interface UserCenterService {
    UserCenterPageVO getCenterPageInfo(Long userId);
}
