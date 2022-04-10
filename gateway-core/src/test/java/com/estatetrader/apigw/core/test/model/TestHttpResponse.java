package com.estatetrader.apigw.core.test.model;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import org.apache.dubbo.rpc.protocol.thrift.io.RandomAccessByteArrayOutputStream;
import com.estatetrader.apigw.core.contracts.GatewayResponse;

import java.io.OutputStream;
import java.util.function.Consumer;

public class TestHttpResponse implements GatewayResponse {

    private RandomAccessByteArrayOutputStream body = new RandomAccessByteArrayOutputStream();

    public String getBody() {
        return body.toString();
    }

    /**
     * 设置响应代码
     *
     * @param code         HTTP响应代码
     * @param reasonPhrase 原因描述
     */
    @Override
    public void setStatus(int code, String reasonPhrase) {

    }

    /**
     * 向客户端发送错误信息
     *
     * @param errorMessage 错误信息
     */
    @Override
    public void sendError(String errorMessage) {

    }

    /**
     * 设置客户端重定向
     *
     * @param location 重定向的目标地址
     */
    @Override
    public void sendRedirect(String location) {

    }

    /**
     * 设置响应头
     *
     * @param headerName  响应头名称
     * @param headerValue 响应头的值
     */
    @Override
    public void setHeader(String headerName, String headerValue) {

    }

    /**
     * 添加响应头（用于重复设置同名头）
     *
     * @param headerName  响应头名称
     * @param headerValue 响应头的值
     */
    @Override
    public void addHeader(String headerName, String headerValue) {

    }

    /**
     * 设置响应的内容格式
     *
     * @param contentType 内容格式
     */
    @Override
    public void setContentType(String contentType) {

    }

    /**
     * 设置响应的编码格式
     *
     * @param encoding 编码格式
     */
    @Override
    public void setContentEncoding(String encoding) {

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

    }

    /**
     * 获取响应体的输出流
     *
     * @return 响应体输出流
     */
    @Override
    public OutputStream getOutputStream() {
        return body;
    }
}
