package com.atguigu.lease.common.springmvc;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理类
 * <p>
 * 原来的实现把「所有」异常都塞进 {@code Result.fail()}，前端只能看到一句"失败"，
 * 真正的原因（NPE、SQL 错误、JSON 解析失败…）全丢了，排查非常痛苦。
 * 现在：
 * 1. 业务异常（LeaseException）原样返回它的 code + message；
 * 2. 常见的框架异常给出明确提示（缺参数、JSON 格式错、参数类型错、校验失败）；
 * 3. 其余未知异常：控制台打印完整堆栈，返回信息里带上「异常类名 + 原始消息」，
 *    这样前端也能看到线索。
 * <p>
 * 注意：生产环境不应把异常内部信息暴露给前端，这里是为了课程/开发阶段便于定位。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：原样返回 */
    @ExceptionHandler(LeaseException.class)
    public Result leaseException(LeaseException e) {
        log.warn("业务异常 [{}] {} ", e.getCode(), e.getMessage());
        return Result.build(e.getCode(), e.getMessage());
    }

    /** 请求体 JSON 解析失败（字段类型不对、日期格式不对等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result httpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败", e);
        return Result.build(ResultCodeEnum.PARAM_ERROR.getCode(), "请求体格式不正确：" + brief(e.getMessage()));
    }

    /** 缺少必填的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result missingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数", e);
        return Result.build(ResultCodeEnum.PARAM_ERROR.getCode(),
                "缺少必填参数：" + e.getParameterName());
    }

    /** 参数类型不匹配（例如 ?leaseStatus=abc） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result argumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配", e);
        return Result.build(ResultCodeEnum.PARAM_ERROR.getCode(),
                "参数 " + e.getName() + " 的值不合法：" + e.getValue());
    }

    /** 参数校验失败（@Valid） */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result validateFailed(BindException e) {
        String message = "参数校验失败";
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            message = fieldError.getField() + " " + fieldError.getDefaultMessage();
        }
        log.warn("参数校验失败：{}", message);
        return Result.build(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /** 兜底：其它未预料的异常 */
    @ExceptionHandler(Exception.class)
    public Result exception(Exception e) {
        // 完整堆栈打到控制台，方便直接定位
        log.error("未处理的异常：{}", e.getClass().getName(), e);

        String detail = e.getClass().getSimpleName();
        if (StringUtils.hasText(e.getMessage())) {
            detail += "：" + brief(e.getMessage());
        }
        return Result.build(ResultCodeEnum.FAIL.getCode(), "系统异常（" + detail + "）");
    }

    /** 异常消息可能很长（SQL 报错会带整条语句），截断一下 */
    private String brief(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }
}
