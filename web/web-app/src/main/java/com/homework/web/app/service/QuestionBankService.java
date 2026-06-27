package com.homework.web.app.service;

import com.homework.web.app.dto.BankQueryDTO;
import com.homework.web.app.dto.QuestionBankSaveDTO;
import com.homework.web.app.vo.CategoryDetailVO;
import com.homework.web.app.vo.QuestionBankListVO;

import java.util.List;

public interface QuestionBankService {
    List<CategoryDetailVO> listPrototypeBanks(BankQueryDTO query);
    List<QuestionBankListVO> listQuestionBanks(BankQueryDTO query);
    Long saveQuestionBank(QuestionBankSaveDTO dto);
    void deleteQuestionBank(Long id);
    java.util.Map<String, Object> favoriteBank(Long userId, Long bankId);
}
