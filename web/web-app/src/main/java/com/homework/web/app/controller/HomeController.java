package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.service.HomeService;
import com.homework.web.app.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public Result<HomeVO> home() {
        return Result.success(homeService.getHome());
    }
}
