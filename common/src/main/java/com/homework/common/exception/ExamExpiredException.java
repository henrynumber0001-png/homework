package com.homework.common.exception;

import com.homework.common.result.ResultCodeEnum;

public class ExamExpiredException extends HomeworkException {

    public ExamExpiredException() {
        super(ResultCodeEnum.EXAM_EXPIRED);
    }
}
