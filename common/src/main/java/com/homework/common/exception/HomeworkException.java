package com.homework.common.exception;

import com.homework.common.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public class HomeworkException extends RuntimeException{

    private final ResultCodeEnum resultCodeEnum;

    public HomeworkException(ResultCodeEnum resultCodeEnum){
        super(resultCodeEnum.getMessage()); //获取枚举常量的字符串信息
        this.resultCodeEnum = resultCodeEnum;

    }
    public HomeworkException(ResultCodeEnum resultCodeEnum, Throwable cause){
        super(resultCodeEnum.getMessage(), cause);
        this.resultCodeEnum = resultCodeEnum;
    }

}
