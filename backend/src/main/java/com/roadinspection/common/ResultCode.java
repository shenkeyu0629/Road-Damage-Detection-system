package com.roadinspection.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USER_EXISTS(1004, "用户名已存在"),
    TASK_NOT_FOUND(2001, "任务不存在"),
    RECORD_NOT_FOUND(2002, "记录不存在"),
    DAMAGE_NOT_FOUND(3001, "病害记录不存在"),
    ALARM_NOT_FOUND(4001, "告警记录不存在"),
    FILE_UPLOAD_ERROR(5001, "文件上传失败"),
    FILE_NOT_FOUND(5002, "文件不存在"),
    AI_SERVICE_ERROR(6001, "AI服务调用失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
