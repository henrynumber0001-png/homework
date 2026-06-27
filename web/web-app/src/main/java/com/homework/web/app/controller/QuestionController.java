package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.service.QuestionService;
import com.homework.web.app.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/by-bank/{bankId}")
    public Result<List<QuestionVO>> byBank(@PathVariable Long bankId) {
        return Result.success(questionService.listQuestionsByBank(bankId));
    }

    @GetMapping("/{questionId}")
    public Result<QuestionVO> detail(@PathVariable Long questionId) {
        return Result.success(questionService.getQuestion(questionId));
    }

    @PostMapping("/{questionId}/favorite")
    public Result<Map<String, Object>> favoriteQuestion(@PathVariable Long questionId, @RequestParam Long userId) {
        return Result.success(questionService.favoriteQuestion(userId, questionId));
    }
}
