package com.homework.web.app.controller.bank;


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
    @PostMapping("/answer")
    public Result<Void> saveExamAnswer(@RequestBody CertificateExamAnswerDTO dto) {
        certificateExamService.saveAnswer(dto);
        return Result.success();
    }

    // 用户主动交卷；Service 会统一判题并返回答案解析。
    @PostMapping("/{sessionId}/submit")
    public Result<BankFinishVO> submitExam(@PathVariable Long sessionId) {
        return Result.success(certificateExamService.submit(sessionId));
    }
}
