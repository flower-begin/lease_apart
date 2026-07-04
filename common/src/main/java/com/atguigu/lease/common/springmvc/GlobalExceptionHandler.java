package com.atguigu.lease.common.springmvc;

import com.atguigu.lease.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理类
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 可以定义处理各种异常的handler
    @ExceptionHandler(Exception.class)
    public Result exception(Exception e) {
        // 在控制台打印异常信息
        e.printStackTrace();
        return Result.fail();
    }
}
