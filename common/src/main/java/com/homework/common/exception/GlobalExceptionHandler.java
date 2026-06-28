package com.homework.common.exception;

import com.homework.common.result.Result;
import com.homework.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HomeworkException.class)
    public Result<Void> handleHomeworkException(HomeworkException e){
        log.error("业务异常：{}", e.getMessage());
        //e.getMessage() 调用的是 Throwable类的 getMessage()方法, 返回的是Throwable类的 detailMessage
        //detailMessage 里面存的是resultCodeEnum.getMessage()，字符串信息
        return Result.fail(e.getResultCodeEnum());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class}) //这里接收的异常，都不是你主动抛出的，而是被动触发的
    public Result<Void> handleException(Exception e){
        log.error("系统异常：{}", e.getMessage(),e);
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}
