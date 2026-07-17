package com.homework.web.app.controller;


import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.UserCenterPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserCenterService userCenterService;

    @GetMapping
    public Result<UserCenterPageVO> centerPageInfo() {
        Long userId = LoginUserHolder.getUserId();
        UserCenterPageVO pageVO = userCenterService.getCenterPageInfo(userId);
        return Result.success(pageVO);
    }
}

