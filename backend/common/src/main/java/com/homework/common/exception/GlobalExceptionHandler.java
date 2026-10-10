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

    /*
    在这里讲一下 在 service层 抛出的异常，是怎么被 全局异常处理类 接住并处理的：
    1.首先明确一个概念，异常并不是一定要抛出（也就是传播），如果你能在当前方法内解决掉异常，比如：返回 null，那么也是可以不用 throw 的。
    即，谁能够真正解决异常，谁就捕获；如果当前层解决不了，就继续向上抛。
    但问题有二：
    1）绝大部分的方法内，没有能力解决这个异常；
    2）即使有能力，最终会形成异常与方法的耦合，不利于软件工程化（以后定时任务、消息消费者或者其他模块调用这个 Service，也必须理解 Result 这种 Web 响应包装）
    因此，更常见的做法是 异常传播机制（不是返回，而是抛出，让上层方法处理）

    2.全局异常处理类，相当于代替了 try-catch块。
    在 Controller层，我们只写 result.success(),也就是没有使用 try-catch块 接住 service层方法调用所抛出的异常。
    那么，当我们在 controller层 完成对 service层的方法调用，且该方法抛出异常之后，因为 controller层没有接，所以该异常继续向上传播，最终被 dispatcherServlet 捕获。
    dispatcherServlet 捕获到异常之后，会调用全局异常处理类的 handleException 方法（因为主动抛出的异常，类型就是 HomeworkException类），将异常传入。
    然后全局异常处理类会将异常处理成 Result 对象，返回给 dispatcherServlet，dispatcherServlet 将 Result 对象返回给前端。
     */

    /*
    整个处理流程是：
    HTTP 请求
      ↓
    DispatcherServlet
      ↓
    UserProfileController.profile()
      ↓
    PublicUserProfileServiceImpl.getProfile()
      ↓
    第 50 行抛出 HomeworkException
      ↑ 异常沿调用栈向上传播
    UserProfileController.profile() 未捕获
      ↑
    DispatcherServlet 捕获
      ↓
    HandlerExceptionResolver 查找异常处理方法
      ↓
    发现了 @ExceptionHandler(HomeworkException.class)
      ↓
    调用 GlobalExceptionHandler.handleHomeworkException()

    关于 @ExceptionHandler(HomeworkException.class)，它不属于 Spring AOP，但它在设计思想上具有 AOP 的“横切关注点”特征。
    从设计思想来看，它集中处理多个 Controller 请求中的异常，避免每个 Controller 重复写 try-catch。
    但技术上不是 Spring AOP，而是 Spring MVC 提供的异常解析机制。
     */

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
