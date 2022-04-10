package com.estatetrader.rule.expire;

public enum ExpireReasonType {
    /**
     * 过期，用于一般的过期原因，返回-360错误码
     */
    EXPIRED,
    /**
     * 单设备登录，返回-310错误码
     */
    SINGLE_DEVICE
}
