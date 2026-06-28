package com.homework.web.app.controller.login;

import com.homework.common.result.Result;
import com.homework.model.entity.UserInfo;
import com.homework.web.app.service.UserInfoService;
import com.homework.web.app.vo.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping("/info")
    public Result<UserInfoVo> getUserInfo() {
        UserInfoVo userInfoVo = userInfoService.getUserInfo();
        return Result.success(userInfoVo);
    }
}
