package com.estatetrader.gateway.log;

/**
 * 网关request日志的记录格式
 * request表示网关接收到一次客户端的http请求后的处理结果
 * 由于日志实际存储时使用的是下划线分割方式命名，所以在此类中我们没有使用驼峰命名法
 */
public class RequestLogEntry {
    public String _env;
    public String call_id;
    public String app_id;
    public String device_id;
    public String user_id;
    public String referer;
    public String client_ip;
    public String user_agent;
    public String request_url;
    public String method;
    public String token;
    public String renewed_token;
    public String extension_token;
    public String access_time;
    public String cost;
    public String code;
    public String return_code;
    public String error_msg;
    public int result_length;
    /**
     * 表示此次签名使用的签名类型：dynamic（动态盐签名）, static（静态盐签名）, none（没有使用签名）
     */
    public String signature_type;
    /**
     * 三方登录授权的bindId
     */
    public long third_party_bind_id;
    /**
     * 此次请求消费的所有backend-message的key列表，多个key之间用逗号隔开
     */
    public String consumed_bm_keys;
}
