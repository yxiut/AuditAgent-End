package com.wb.audit.common.result;

/**
 * 统一错误码
 */
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(40001, "参数错误"),
    NO_PERMISSION(40301, "无权限创建任务"),
    NOT_FOUND(40401, "数据不存在"),
    TREE_INVALID(40901, "分派树不合法（条款无叶子路径）"),
    NOTIFY_ERROR(50001, "企微通知失败"),
    SERVER_ERROR(50000, "服务异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
