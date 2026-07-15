package com.homework.web.app.controller;


import com.homework.common.result.Result;
import com.homework.web.app.vo.UserCenterPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    public Result<UserCenterPageVO> centerPageInfo(){

    }
}
