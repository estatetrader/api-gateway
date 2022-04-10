package com.estatetrader.core;

import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;

/**
 * 表示网关在处理请求时遇到的异常，携带有error code用于返回给客户端具体的错误代码
 */
public class GatewayException extends Exception {
    private final AbstractReturnCode code;

    public GatewayException(AbstractReturnCode returnCode, String message, Throwable t) {
        super("Code #" + returnCode.getCode() + ": " + returnCode.getDisplay().getDesc() + (message != null ? ". " + message : ""), t);
        this.code = returnCode;
    }

    public GatewayException(AbstractReturnCode returnCode) {
        this(returnCode, null, null);
    }

    public GatewayException(AbstractReturnCode returnCode, Throwable throwable) {
        this(returnCode, null, throwable);
    }

    public GatewayException(String message) {
        this(ApiReturnCode.UNKNOWN_ERROR, message, null);
    }

    public GatewayException(String message, Throwable t) {
        this(ApiReturnCode.UNKNOWN_ERROR, message, t);
    }

    public AbstractReturnCode getCode() {
        return code;
    }
}
