package com.homework.web.app.controller;


import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.model.enums.GroupType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.mapper.InterviewQuestionInfoMapper;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.UserCenterPageVO;
import com.homework.web.app.vo.WrongQuestionBankVO;
import com.homework.web.app.vo.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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


    @GetMapping("/wrong-question-banks")
    public Result<PageResult<WrongQuestionBankVO>> wrongQuestionBanks(@RequestParam GroupType groupType,
                                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                                      @RequestParam(defaultValue = "20") Integer pageSize) {

        return Result.success(userCenterService.getWrongQuestionBanks(LoginUserHolder.getUserId(), groupType, pageNum, pageSize));
    }

    @GetMapping("/wrong-questions")
    public Result<PageResult<WrongQuestionVO>> wrongQuestions(@RequestParam Long bankId,
                                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "20") Integer pageSize){
        return Result.success(userCenterService.getWrongQuestions(LoginUserHolder.getUserId(),bankId,pageNum, pageSize));
    }
}

