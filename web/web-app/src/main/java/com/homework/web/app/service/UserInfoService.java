package com.homework.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.model.entity.UserInfo;
import com.homework.web.app.vo.UserInfoVo;

public interface UserInfoService extends IService<UserInfo> {

    UserInfoVo getUserInfo();
}
