package com.homework.web.app.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.PublicUserProfileService;
import com.homework.web.app.vo.FollowerVO;
import com.homework.web.app.vo.HitPostVO;
import com.homework.web.app.vo.PublicUserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/app/users")
@RequiredArgsConstructor
public class UserProfileController {
    private final PublicUserProfileService profileService;

    @GetMapping("/{userId}/profile")
    public Result<PublicUserProfileVO> profile(@PathVariable Long userId) {
        return Result.success(profileService.getProfile(LoginUserHolder.getUserId(), userId));
    }

    @GetMapping("/{userId}/profile/posts")
    public Result<List<HitPostVO>> posts(@PathVariable Long userId,
                                         @RequestParam(required = false) Integer pageNum,
                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(profileService.listPosts(LoginUserHolder.getUserId(), userId, pageNum, pageSize));
    }



}
