package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.service.ProfileService;
import com.homework.web.app.vo.ProfileOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}/overview")
    public Result<ProfileOverviewVO> overview(@PathVariable Long userId) {
        return Result.success(profileService.getOverview(userId));
    }

    @GetMapping("/{userId}/wrong-questions")
    public Result<List<Map<String, Object>>> wrongQuestions(@PathVariable Long userId) {
        return Result.success(profileService.listWrongQuestions(userId));
    }

    @GetMapping("/{userId}/favorite-banks")
    public Result<List<Map<String, Object>>> favoriteBanks(@PathVariable Long userId) {
        return Result.success(profileService.listFavoriteBanks(userId));
    }

    @GetMapping("/{userId}/favorite-questions")
    public Result<List<Map<String, Object>>> favoriteQuestions(@PathVariable Long userId) {
        return Result.success(profileService.listFavoriteQuestions(userId));
    }

    @GetMapping("/{userId}/notes")
    public Result<List<Map<String, Object>>> notes(@PathVariable Long userId) {
        return Result.success(profileService.listNotes(userId));
    }
}
