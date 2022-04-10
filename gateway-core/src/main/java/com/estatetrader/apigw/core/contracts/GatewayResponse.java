package com.estatetrader.apigw.core.contracts;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * 网关响应
 */
public interface GatewayResponse {

    /**
     * 设置响应代码
     * @param code HTTP响应代码
     * @param reasonPhrase 原因描述
     */
    void setStatus(int code, String reasonPhrase);

    /**
     * 向客户端发送错误信息
     *
     * @param errorMessage 错误信息
     */
    void sendError(String errorMessage);

    /**
     * 设置客户端重定向
     * @param location 重定向的目标地址
     */
    void sendRedirect(String location);

    /**
     * 向客户端返回304/Not Modified，用于ETag缓存
     */
    default void sendNotModified() {
        setStatus(304, "Not Modified");
    }

    /**
     * 通知客户端所请求的资源不存在
     */
    default void sendNotFound() {
        setStatus(404, "File Not Found");
    }

    /**
     * 设置响应头
     * @param headerName 响应头名称
     * @param headerValue 响应头的值
     */
    void setHeader(String headerName, String headerValue);

    /**
     * 添加响应头（用于重复设置同名头）
     * @param headerName 响应头名称
     * @param headerValue 响应头的值
     */
    void addHeader(String headerName, String headerValue);

    /**
     * 设置响应的内容格式
     * @param contentType 内容格式
     */
    void setContentType(String contentType);

    /**
     * 设置响应的编码格式
     * @param encoding 编码格式
     */
    void setContentEncoding(String encoding);

    /**
     * 设置此次响应的ETag，用于客户端缓存
     * @param etag etag
     */
    default void setETag(String etag) {
        setHeader("ETag", etag);
    }

    /**
     * 向客户端种植cookie
     * @param cookieName cookie名称
     * @param cookieValue cookie的值
     */
    default void setCookie(String cookieName, String cookieValue) {
        setCookie(cookieName, cookieValue, null);
    }

    /**
     * 向客户端种植cookie
     * @param cookieName cookie名称
     * @param cookieValue cookie的值
     * @param settings cookie的其他配置
     */
    void setCookie(String cookieName, String cookieValue, Consumer<GatewayCookie> settings);

    /**
     * 获取响应体的输出流
     * @return 响应体输出流
     */
    OutputStream getOutputStream();
}
