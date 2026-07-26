package com.homework.web.app.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.app.mapper.QuestionBankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 阻止 App 新请求继续访问后台已下架或尚未发布的题库。 */
@Service
@RequiredArgsConstructor
public class PublishedQuestionBankAccessService {

    private final QuestionBankMapper questionBankMapper;

    public void requirePublished(Long bankId) {
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null || bank.getStatus() != QuestionBankStatus.PUBLISHED) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
    }
}
