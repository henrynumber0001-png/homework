package com.homework.web.app.controller.bank;

import com.homework.common.result.Result;
import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.GroupType;
import com.homework.web.app.dto.AiFollowUpDTO;
import com.homework.web.app.dto.CertificateQuestionSubmitDTO;
import com.homework.web.app.dto.InterviewQuestionSubmitDTO;
import com.homework.web.app.dto.UserQuestionNoteDTO;
import com.homework.web.app.service.QuestionInfoService;
import com.homework.web.app.vo.*;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/app/bank/questions")
public class QuestionInfoController {

    private final QuestionInfoService questionInfoService;

    //用于返回问题给前端
    @GetMapping("/interview/question")
    public Result<List<InterviewQuestionPageVO>> getQuestionsByBankId(@RequestParam Long bankId) {
       List<InterviewQuestionPageVO> questions = questionInfoService.getQuestionsByBankId(bankId);
       return Result.success(questions);
    }

    //用于返回AI反馈和参考答案给前端
    @PostMapping("/interview/answer")
    public Result<InterViewAnswerPageVO> getInterviewAnswer(@RequestBody InterviewQuestionSubmitDTO submitDTO){
        InterViewAnswerPageVO answer = questionInfoService.getInterviewAnswer(submitDTO);
        return Result.success(answer);
    }
    @PostMapping("/answer/note")
    public Result<Void> saveUserQuestionNote(@RequestBody UserQuestionNoteDTO noteDTO){
        questionInfoService.saveUserQuestionNote(noteDTO);
        return Result.success();
    }

    //认证题库 -> 练习模式
    @GetMapping("/certificate/question")
    public Result<List<CertificateQuestionPageVO>> getCertByPractice(@RequestParam Long bankId) {
        List<CertificateQuestionPageVO> questions = questionInfoService.getCertificateByBankId(bankId);
        return Result.success(questions);
    }

    //认证题库 -> 练习模式 -> 获得答案
    @PostMapping("/certificate/practice/answer")
    public Result<CertificateAnswerPageVO> getCertificateAnswer(@RequestBody CertificateQuestionSubmitDTO submitDTO) {
        CertificateAnswerPageVO answer = questionInfoService.getCertificateAnswer(submitDTO);
        return Result.success(answer);
    }

    @GetMapping("/interview/review")
    public Result<List<InterviewQuestionReviewVO>> getInterviewQuestionReview(@RequestParam Long bankId) {
        List<InterviewQuestionReviewVO> questions = questionInfoService.getInterviewQuestionReview(bankId);
        return Result.success(questions);
    }

    @GetMapping("certificate/review")
    public Result<List<CertificateQuestionReviewVO>> getCertificateQuestionReview(@RequestParam Long bankId){
        List<CertificateQuestionReviewVO> questions = questionInfoService.getCertificateQuestionReview(bankId);
        return Result.success(questions);
    }

    @GetMapping("/certificate/record")
    public Result<List<CertificateQuestionReviewVO>> getCertificateRecord(@RequestParam Long bankId) {
        List<CertificateQuestionReviewVO> records = questionInfoService.getCertificateRecord(bankId);
        return Result.success(records);
    }

    @GetMapping("/interview/record")
    public Result<List<InterviewQuestionReviewVO>> getInterviewRecord(@RequestParam Long bankId) {
        List<InterviewQuestionReviewVO> records = questionInfoService.getInterviewRecord(bankId);
        return Result.success(records);
    }

    //给用户的功能：清除当前所答题目的全部记录
    @DeleteMapping("/clear/record")
    public Result<Void> clearRecord(@RequestParam Long bankId, @RequestParam GroupType groupType){
        questionInfoService.clearRecord(bankId,groupType);
        return Result.success();
    }

    @PostMapping("/finish")
    public Result<BankFinishVO> finishBank(@RequestParam Long bankId, @RequestParam GroupType groupType){
        BankFinishVO finishVO = questionInfoService.finishBank(bankId,groupType);
        return Result.success(finishVO);
    }

    //用户点击答案解析里的“追问AI”按钮时，先调用这个接口拉取当前题库已有的 AI 会话。
    //如果之前已经追问过，会返回历史消息；如果还没追问过，会返回空消息列表。
    @GetMapping("/ai/chat")
    public Result<AiChatVO> startAiChat(@RequestParam Long bankId, @RequestParam GroupType bankType) {
        AiChatVO vo = questionInfoService.startAiChat(bankId, bankType);
        return Result.success(vo);
    }

    //用户在“追问AI”弹窗中输入问题后，调用这个接口。
    //后端会保存用户问题、携带题目解析和历史消息调用 AI、再保存 AI 回复并返回完整会话。
    @PostMapping("/ai/chat")
    public Result<AiChatVO> followUpAi(@RequestBody AiFollowUpDTO dto) {
        AiChatVO vo = questionInfoService.followUpAi(dto);
        return Result.success(vo);
    }

    //前端主动调用 closeAiChat
    @PostMapping("/ai/chat/close")
    public Result<Void> closeAiChat(@RequestParam Long bankId) {
        questionInfoService.closeAiChat(bankId);
        return Result.success();
    }

    //收藏题目
    @PostMapping("/collect")
    public Result<Void> collect(@RequestParam Long bankId,
                                @RequestParam Long questionId,
                                @RequestParam ActionStatus actionStatus) {
        questionInfoService.collect(bankId,questionId,actionStatus);
        return Result.success();
    }
}
