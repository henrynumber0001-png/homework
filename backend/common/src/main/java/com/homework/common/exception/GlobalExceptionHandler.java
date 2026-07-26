package com.homework.common.exception;

import com.homework.common.result.Result;
import com.homework.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class
    })
    public Result<Void> handleException(Exception e){
        // 参数校验失败属于客户端可修正的问题，不应伪装成系统故障。
        log.warn("参数异常：{}", e.getMessage());
        Result<Void> result = Result.fail(ResultCodeEnum.PARAM_ERROR);
        // 将“最多 140 字”等可操作提示返回给前端，便于直接展示给用户。
        result.setMessage(e.getMessage());
        return result;
    }
}
