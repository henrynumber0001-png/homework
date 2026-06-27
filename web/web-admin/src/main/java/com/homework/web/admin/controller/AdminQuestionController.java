package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.QuestionSaveDTO;
import com.homework.web.app.service.QuestionService;
import com.homework.web.app.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

    @GetMapping("/by-bank/{bankId}")
    public Result<List<QuestionVO>> byBank(@PathVariable Long bankId) {
        return Result.success(questionService.listQuestionsByBank(bankId));
    }

    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.success(questionService.getQuestion(id));
    }

    @PostMapping
    public Result<Long> save(@RequestBody QuestionSaveDTO dto) {
        return Result.success(questionService.saveQuestion(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success();
    }
}
