package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.PublicUserProfileService;
import com.homework.web.app.vo.MentionUserVO;
import com.homework.web.app.vo.PublicUserProfileActivityVO;
import com.homework.web.app.vo.PublicUserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/app/users")
@RequiredArgsConstructor
public class UserProfileController {
    private final PublicUserProfileService profileService;

    @GetMapping("/search")
    public Result<List<MentionUserVO>> search(@RequestParam String keyword,
                                              @RequestParam(required = false) Integer limit) {
        return Result.success(profileService.searchUsers(LoginUserHolder.getUserId(), keyword, limit));
    }

    @GetMapping("/{userId}/profile")
    public Result<PublicUserProfileVO> profile(@PathVariable Long userId) {
        return Result.success(profileService.getProfile(LoginUserHolder.getUserId(), userId));
    }

    @GetMapping("/{userId}/profile/activities")
    public Result<List<PublicUserProfileActivityVO>> activities(@PathVariable Long userId,
                                                                @RequestParam(defaultValue = "posts") String tab,
                                                                @RequestParam(required = false) Integer pageNum,
                                                                @RequestParam(required = false) Integer pageSize) {
        return Result.success(profileService.listActivities(LoginUserHolder.getUserId(), userId, tab, pageNum, pageSize));
    }
}
