package com.wb.audit.common.exception;

import com.wb.audit.common.result.ResultCode;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {

    private final int code;

    public int getCode() { return code; }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}

