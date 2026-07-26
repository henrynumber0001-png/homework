package com.homework.web.admin.exception;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.Result;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.admin.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将管理端异常转换为统一 Result 响应。 */
@RestControllerAdvice
@RequiredArgsConstructor
public class AdminExceptionHandler {

    private final AdminAuditService auditService;

    @ExceptionHandler(HomeworkException.class)
    public Result<Void> handleHomeworkException(HomeworkException exception) {
        auditService.recordFailure(exception.getMessage());
        return Result.fail(exception.getResultCodeEnum());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleParameterException(Exception exception) {
        auditService.recordFailure(exception.getMessage());
        Result<Void> result = Result.fail(ResultCodeEnum.PARAM_ERROR);
        result.setMessage(exception.getMessage());
        return result;
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception exception) {
        auditService.recordFailure(exception.getMessage());
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}
