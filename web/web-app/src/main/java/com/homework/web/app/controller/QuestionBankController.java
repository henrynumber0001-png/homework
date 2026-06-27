package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.BankQueryDTO;
import com.homework.web.app.service.QuestionBankService;
import com.homework.web.app.vo.CategoryDetailVO;
import com.homework.web.app.vo.QuestionBankListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/question-banks")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping("/prototype")
    public Result<List<CategoryDetailVO>> prototypeBanks(BankQueryDTO query) {
        return Result.success(questionBankService.listPrototypeBanks(query));
    }

    @GetMapping
    public Result<List<QuestionBankListVO>> questionBanks(BankQueryDTO query) {
        return Result.success(questionBankService.listQuestionBanks(query));
    }

    @PostMapping("/{bankId}/favorite")
    public Result<Map<String, Object>> favoriteBank(@PathVariable Long bankId, @RequestParam Long userId) {
        return Result.success(questionBankService.favoriteBank(userId, bankId));
    }
}
