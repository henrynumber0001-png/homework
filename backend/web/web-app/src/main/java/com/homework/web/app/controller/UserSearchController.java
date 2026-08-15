package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.SearchUsersService;
import com.homework.web.app.vo.MentionUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/users")
public class UserSearchController {

    private final SearchUsersService searchUsersService;

    @GetMapping("/search")
    public Result<List<MentionUserVO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.success(searchUsersService.searchUsers(LoginUserHolder.getUserId(), keyword, limit));
    }
}
