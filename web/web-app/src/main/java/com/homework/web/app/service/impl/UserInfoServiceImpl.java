package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.common.enums.UserInfoStatus;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.utils.LoginUserHolder;
import com.homework.model.entity.UserInfo;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.service.UserInfoService;
import com.homework.web.app.vo.UserInfoVo;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfoVo getUserInfo() {

        Long userId = LoginUserHolder.getUserId();

        if(userId == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
    }

        UserInfo userInfo = this.getById(userId);
        if(userInfo == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }

        if(userInfo.getStatus() != UserInfoStatus.ACTIVE){
            throw new HomeworkException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
        }

        UserInfoVo userInfoVo = new UserInfoVo();
        userInfoVo.setAccountNo(userInfo.getAccountNo());
        userInfoVo.setAvatar(userInfo.getAvatar());
        userInfoVo.setDisplayName(userInfo.getDisplayName());

        return userInfoVo;
    }
}
