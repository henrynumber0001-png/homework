package com.homework.web.app.controller;


import com.homework.common.result.Result;
import com.homework.web.app.dto.CertificateExamAnswerDTO;
import com.homework.web.app.service.CertificateExamService;
import com.homework.web.app.vo.BankFinishVO;
import com.homework.web.app.vo.CertificateExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/bank/certificate/exams")
public class CertificateExamController {

    // Controller 只接收请求；具体考试规则全部交给 Service 处理。
    private final CertificateExamService certificateExamService;

    // 用户第一次进入时创建考试；尚有进行中的考试时恢复原场次。
    @PostMapping("/start")
    public Result<CertificateExamVO> startExam(@RequestParam Long bankId) {
        return Result.success(certificateExamService.startOrResume(bankId));
    }

    // 浏览器刷新或用户重新进入时，通过 sessionId 恢复考试。
    @GetMapping("/{sessionId}")
    public Result<CertificateExamVO> getSession(@PathVariable Long sessionId) {
        return Result.success(certificateExamService.getSession(sessionId));
    }

    // 用户选择或取消选项时，覆盖保存这道题的临时答案。
    @PutMapping("/{sessionId}/questions/{questionId}/answer")
    public Result<Void> saveExamAnswer(@PathVariable Long sessionId,
                                       @PathVariable Long questionId, @RequestBody CertificateExamAnswerDTO dto) {
        certificateExamService.saveAnswer(sessionId, questionId, dto);
        return Result.success();
    }

    // 用户主动交卷；Service 会统一判题并返回答案解析。
    @PostMapping("/{sessionId}/submit")
    public Result<BankFinishVO> submitExam(@PathVariable Long sessionId) {
        return Result.success(certificateExamService.submit(sessionId));
    }

    // 用户明确点击“放弃考试”时结束本场考试，不删除历史场次。
    @PostMapping("/{sessionId}/abandon")
    public Result<Void> abandonExam(@PathVariable Long sessionId) {
        certificateExamService.abandon(sessionId);
        return Result.success();
    }
}
