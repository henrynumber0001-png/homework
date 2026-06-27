package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.PracticeSessionCreateDTO;
import com.homework.web.app.dto.QuestionAnswerSubmitDTO;
import com.homework.web.app.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/app/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(@RequestBody PracticeSessionCreateDTO dto) {
        return Result.success(practiceService.createSession(dto));
    }

    @PostMapping("/answers")
    public Result<Map<String, Object>> submitAnswer(@RequestBody QuestionAnswerSubmitDTO dto) {
        return Result.success(practiceService.submitAnswer(dto));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public Result<Map<String, Object>> submitSession(@PathVariable Long sessionId) {
        return Result.success(practiceService.submitSession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/result")
    public Result<Map<String, Object>> sessionResult(@PathVariable Long sessionId) {
        return Result.success(practiceService.getSessionResult(sessionId));
    }
}
