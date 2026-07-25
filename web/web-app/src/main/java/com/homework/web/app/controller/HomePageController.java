package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.service.HitService;
import com.homework.web.app.service.HomePageService;
import com.homework.web.app.vo.HomePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/home-page")
public class HomePageController {
    private final HomePageService homePageService;


    @GetMapping
    public Result<HomePageVO> getHomePage() {
       HomePageVO homePageVO = homePageService.getHomePage();
        return Result.success(homePageVO);
    }



}
