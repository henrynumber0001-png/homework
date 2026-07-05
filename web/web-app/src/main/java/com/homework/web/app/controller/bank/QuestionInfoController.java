package com.homework.web.app.controller.bank;

import com.homework.common.result.Result;
import com.homework.model.entity.UserQuestionNote;
import com.homework.web.app.dto.InterviewQuestionSubmitDTO;
import com.homework.web.app.dto.UserQuestionNoteDTO;
import com.homework.web.app.service.QuestionInfoService;
import com.homework.web.app.vo.InterViewAnswerPageVO;
import com.homework.web.app.vo.InterviewQuestionPageVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/app/bank/question-info")
public class QuestionInfoController {

    private final QuestionInfoService questionInfoService;

    //这个部分用于返回问题给前端
    @GetMapping("/question-part")
    public Result<List<InterviewQuestionPageVO>> getQuestionsByBankId(@RequestParam Long bankId) {
       List<InterviewQuestionPageVO> questions = questionInfoService.getQuestionsByBankId(bankId);
       return Result.success(questions);
    }

    @PostMapping("/answer")
    public Result<InterViewAnswerPageVO> getAnswer(@RequestBody InterviewQuestionSubmitDTO submitDTO){
        InterViewAnswerPageVO answer = questionInfoService.getAnswer(submitDTO);
        return Result.success(answer);
    }
    @PostMapping("/answer/note")
    public Result<Void> saveUserQuestionNote(@RequestBody UserQuestionNoteDTO noteDTO){
        questionInfoService.saveUserQuestionNote(noteDTO);
        return Result.success();
    }
}
