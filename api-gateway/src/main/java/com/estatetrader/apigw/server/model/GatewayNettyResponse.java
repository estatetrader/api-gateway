package com.estatetrader.apigw.server.model;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class GatewayNettyResponse implements GatewayResponse {
    private final FullHttpResponse response;

    public GatewayNettyResponse(FullHttpResponse response) {
        this.response = response;
    }

    /**
     * 设置响应代码
     *
     * @param code    HTTP响应代码
     * @param reasonPhrase 原因描述
     */
    @Override
    public void setStatus(int code, String reasonPhrase) {
        response.setStatus(new HttpResponseStatus(code, reasonPhrase));
    }

    /**
     * 向客户端发送错误信息
     *
     * @param errorMessage 错误信息
     */
    @Override
    public void sendError(String errorMessage) {
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        response.content().clear().writeCharSequence(errorMessage, StandardCharsets.UTF_8);
    }

    /**
     * 设置客户端重定向
     *
     * @param location 重定向的目标地址
     */
    @Override
    public void sendRedirect(String location) {
        response.headers().set(HttpHeaderNames.LOCATION, location);
        response.setStatus(HttpResponseStatus.FOUND);
    }

    /**
     * 设置响应头
     *
     * @param headerName  响应头名称
     * @param headerValue 响应头的值
     */
    @Override
    public void setHeader(String headerName, String headerValue) {
        response.headers().set(headerName, headerValue);
    }

    /**
     * 添加响应头（用于重复设置同名头）
     *
     * @param headerName  响应头名称
     * @param headerValue 响应头的值
     */
    @Override
    public void addHeader(String headerName, String headerValue) {
        response.headers().add(headerName, headerValue);
    }

    /**
     * 设置响应的内容格式
     *
     * @param contentType 内容格式
     */
    @Override
    public void setContentType(String contentType) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    /**
     * 设置响应的编码格式
     *
     * @param encoding 编码格式
     */
    @Override
    public void setContentEncoding(String encoding) {
        response.headers().set(HttpHeaderNames.CONTENT_ENCODING, encoding);
    }

    /**
     * 向客户端种植cookie
     *
     * @param cookieName  cookie名称
     * @param cookieValue cookie的值
     * @param settings    cookie的其他配置
     */
    @Override
    public void setCookie(String cookieName, String cookieValue, Consumer<GatewayCookie> settings) {
        Cookie cookie = new DefaultCookie(cookieName, cookieValue);
        if (settings != null) {
            settings.accept(new GatewayNettyCookie(cookie));
        }
        String headerValue = ServerCookieEncoder.STRICT.encode(cookie);
        response.headers().add(HttpHeaderNames.SET_COOKIE, headerValue);
    }

    /**
     * 获取响应体的输出流
     *
     * @return 响应体输出流
     */
    @Override
    public OutputStream getOutputStream() {
        return new ByteBufOutputStream(response.content());
    }
}
