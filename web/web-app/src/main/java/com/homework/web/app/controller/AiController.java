package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.AiChatMessageDTO;
import com.homework.web.app.dto.CorrectionFeedbackDTO;
import com.homework.web.app.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/app/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody AiChatMessageDTO dto) {
        return Result.success(aiService.ask(dto));
    }

    @PostMapping("/corrections")
    public Result<Long> correction(@RequestBody CorrectionFeedbackDTO dto) {
        return Result.success(aiService.submitCorrection(dto));
    }
}
