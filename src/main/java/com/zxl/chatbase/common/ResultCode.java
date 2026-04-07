package com.zxl.chatbase.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    ERROR(500, "系统错误"),
    
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    BUSINESS_ERROR(1000, "业务错误"),
    DATA_NOT_FOUND(1001, "数据不存在"),
    DATA_ALREADY_EXISTS(1002, "数据已存在"),
    VALIDATION_FAILED(1003, "验证失败"),
    
    DATABASE_ERROR(2001, "数据库错误"),
    NETWORK_ERROR(2002, "网络错误"),
    TIMEOUT_ERROR(2003, "超时错误");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
