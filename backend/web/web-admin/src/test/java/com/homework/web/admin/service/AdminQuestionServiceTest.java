package com.homework.web.admin.service;

import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionAction;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.dto.QuestionActionDTO;
import com.homework.web.admin.dto.QuestionNoUpdateDTO;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.vo.QuestionNoUpdateResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminQuestionServiceTest {

    @Mock
    private QuestionBankMapper bankMapper;
    @Mock
    private InterviewQuestionMapper interviewQuestionMapper;
    @Mock
    private CertificateQuestionMapper certificateQuestionMapper;
    @Mock
    private AdminAccessService accessService;
    @Mock
    private QuestionContentService contentService;
    @Mock
    private QuestionImageService imageService;
    @Mock
    private QuestionAssembler assembler;
    @Mock
    private AdminAuditService auditService;
    @InjectMocks
    private AdminQuestionService service;

    @Test
    void movingInterviewQuestionUpShiftsIntermediateQuestionsBack() {
        QuestionBank bank = bank(4);
        QuestionBank updatedBank = bank(5);
        InterviewQuestionInfo question = new InterviewQuestionInfo();
        question.setId(88L);
        question.setBankId(11L);
        question.setQuestionNo(8);
        QuestionNoUpdateDTO dto = updateDto(3, 4);

        when(bankMapper.selectForUpdate(11L)).thenReturn(bank);
        when(bankMapper.selectGroupType(11L)).thenReturn(GroupType.INTERVIEW);
        when(interviewQuestionMapper.selectActiveForUpdate(11L, 88L)).thenReturn(question);
        when(interviewQuestionMapper.selectActiveQuestionCount(11L)).thenReturn(8);
        when(bankMapper.bumpVersion(11L, 4)).thenReturn(1);
        when(interviewQuestionMapper.parkQuestionNo(11L, 88L, 8)).thenReturn(1);
        when(interviewQuestionMapper.negateQuestionNoRange(11L, 3, 7)).thenReturn(5);
        when(interviewQuestionMapper.restoreQuestionNoRange(11L, 3, 7, 1)).thenReturn(5);
        when(interviewQuestionMapper.placeQuestionNo(11L, 88L, 3)).thenReturn(1);
        when(bankMapper.selectById(11L)).thenReturn(updatedBank);

        QuestionNoUpdateResultVO result = service.updateQuestionNo(11L, 88L, dto);

        assertEquals(8, result.getPreviousQuestionNo());
        assertEquals(3, result.getQuestionNo());
        assertEquals(5, result.getBankQuestionOrderVersion());
        verify(interviewQuestionMapper).negateQuestionNoRange(11L, 3, 7);
        verify(interviewQuestionMapper).restoreQuestionNoRange(11L, 3, 7, 1);
    }

    @Test
    void movingCertificateQuestionDownShiftsIntermediateQuestionsForward() {
        QuestionBank bank = bank(9);
        QuestionBank updatedBank = bank(10);
        CertificateQuestionInfo question = new CertificateQuestionInfo();
        question.setId(33L);
        question.setBankId(22L);
        question.setQuestionNo(3);
        QuestionNoUpdateDTO dto = updateDto(8, 9);

        when(bankMapper.selectForUpdate(22L)).thenReturn(bank);
        when(bankMapper.selectGroupType(22L)).thenReturn(GroupType.CERTIFICATION);
        when(certificateQuestionMapper.selectActiveForUpdate(22L, 33L)).thenReturn(question);
        when(certificateQuestionMapper.selectActiveQuestionCount(22L)).thenReturn(8);
        when(bankMapper.bumpVersion(22L, 9)).thenReturn(1);
        when(certificateQuestionMapper.parkQuestionNo(22L, 33L, 3)).thenReturn(1);
        when(certificateQuestionMapper.negateQuestionNoRange(22L, 4, 8)).thenReturn(5);
        when(certificateQuestionMapper.restoreQuestionNoRange(22L, 4, 8, -1)).thenReturn(5);
        when(certificateQuestionMapper.placeQuestionNo(22L, 33L, 8)).thenReturn(1);
        when(bankMapper.selectById(22L)).thenReturn(updatedBank);

        QuestionNoUpdateResultVO result = service.updateQuestionNo(22L, 33L, dto);

        assertEquals(3, result.getPreviousQuestionNo());
        assertEquals(8, result.getQuestionNo());
        assertEquals(10, result.getBankQuestionOrderVersion());
        verify(certificateQuestionMapper).negateQuestionNoRange(22L, 4, 8);
        verify(certificateQuestionMapper).restoreQuestionNoRange(22L, 4, 8, -1);
    }

    @Test
    void unchangedQuestionNumberDoesNotBumpBankVersion() {
        QuestionBank bank = bank(6);
        InterviewQuestionInfo question = new InterviewQuestionInfo();
        question.setId(44L);
        question.setBankId(11L);
        question.setQuestionNo(2);
        QuestionNoUpdateDTO dto = updateDto(2, 6);

        when(bankMapper.selectForUpdate(11L)).thenReturn(bank);
        when(bankMapper.selectGroupType(11L)).thenReturn(GroupType.INTERVIEW);
        when(interviewQuestionMapper.selectActiveForUpdate(11L, 44L)).thenReturn(question);
        when(interviewQuestionMapper.selectActiveQuestionCount(11L)).thenReturn(4);
        when(bankMapper.selectById(11L)).thenReturn(bank);

        QuestionNoUpdateResultVO result = service.updateQuestionNo(11L, 44L, dto);

        assertEquals(6, result.getBankQuestionOrderVersion());
        verify(bankMapper, never()).bumpVersion(11L, 6);
        verify(interviewQuestionMapper, never()).parkQuestionNo(11L, 44L, 2);
    }

    @Test
    void deletingQuestionCompactsFollowingQuestionNumbers() {
        QuestionBank bank = bank(12);
        InterviewQuestionInfo question = new InterviewQuestionInfo();
        question.setId(55L);
        question.setBankId(11L);
        question.setQuestionNo(3);
        question.setVersion(2);
        question.setIsReleased(false);
        InterviewQuestionInfo deleted = new InterviewQuestionInfo();
        deleted.setId(55L);
        deleted.setBankId(11L);
        deleted.setQuestionNo(3);
        deleted.setVersion(3);
        deleted.setIsReleased(false);
        deleted.setDeleted(true);
        deleted.setUpdatedTime(LocalDateTime.of(2026, 8, 3, 1, 5));
        QuestionActionDTO dto = new QuestionActionDTO();
        dto.setAction(QuestionAction.DELETE);
        dto.setReason("删除无效题目");
        dto.setVersion(2);

        when(bankMapper.selectForUpdate(11L)).thenReturn(bank);
        when(bankMapper.selectGroupType(11L)).thenReturn(GroupType.INTERVIEW);
        when(interviewQuestionMapper.selectOne(any())).thenReturn(question);
        when(interviewQuestionMapper.logicalDelete(11L, 55L, 2)).thenReturn(1);
        when(interviewQuestionMapper.selectMaxQuestionNo(11L)).thenReturn(8);
        when(interviewQuestionMapper.negateQuestionNoRange(11L, 4, 8)).thenReturn(5);
        when(interviewQuestionMapper.restoreQuestionNoRange(11L, 4, 8, -1)).thenReturn(5);
        when(bankMapper.bumpVersion(11L, 12)).thenReturn(1);
        when(interviewQuestionMapper.selectIncludingDeleted(11L, 55L)).thenReturn(deleted);

        service.action(11L, 55L, dto);

        verify(interviewQuestionMapper).negateQuestionNoRange(11L, 4, 8);
        verify(interviewQuestionMapper).restoreQuestionNoRange(11L, 4, 8, -1);
        verify(bankMapper).bumpVersion(11L, 12);
    }

    private QuestionNoUpdateDTO updateDto(int questionNo, int bankVersion) {
        QuestionNoUpdateDTO dto = new QuestionNoUpdateDTO();
        dto.setQuestionNo(questionNo);
        dto.setBankQuestionOrderVersion(bankVersion);
        dto.setReason("调整题目章节顺序");
        return dto;
    }

    private QuestionBank bank(int version) {
        QuestionBank bank = new QuestionBank();
        bank.setId(11L);
        bank.setVersion(version);
        bank.setUpdatedTime(LocalDateTime.of(2026, 8, 3, 1, 0));
        return bank;
    }
}
