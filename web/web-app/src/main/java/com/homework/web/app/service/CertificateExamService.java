package com.homework.web.app.service;

import com.homework.web.app.dto.CertificateExamAnswerDTO;
import com.homework.web.app.vo.BankFinishVO;
import com.homework.web.app.vo.CertificateExamVO;

public interface CertificateExamService {

    /** 开始新考试，或者恢复当前尚未结束的考试。 */
    CertificateExamVO startOrResume(Long bankId);

    /** 根据考试场次 id 恢复题目顺序、用户选择和剩余时间。 */
    CertificateExamVO getSession(Long sessionId);

    /** 暂存考试中某一道题的用户选择。 */
    void saveAnswer(Long sessionId, Long questionId, CertificateExamAnswerDTO dto);

    /** 提交试卷并返回完整结算结果；重复提交时返回相同结果。 */
    BankFinishVO submit(Long sessionId);

    /** 用户主动放弃尚未结束的考试。 */
    void abandon(Long sessionId);
}
