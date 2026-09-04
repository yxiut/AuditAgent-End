package com.wb.audit.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.wb.audit.common.result.Result;
import com.wb.audit.common.result.ResultCode;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("接口不存在: {}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在: " + e.getResourcePath());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        String detail = cause.getMessage() == null ? e.getMessage() : cause.getMessage();
        log.warn("请求体解析失败: {}", detail);
        return Result.fail(ResultCode.PARAM_ERROR, "请求体解析失败: " + detail);
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
                Throwable cause = e.getCause() == null ? e : e.getCause();
        String msg = cause.getMessage() == null ? e.getClass().getSimpleName() : cause.getMessage();
        return Result.fail(ResultCode.SERVER_ERROR, "服务异常: " + msg);
    }

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

}
