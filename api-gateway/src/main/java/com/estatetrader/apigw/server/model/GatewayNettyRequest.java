package com.estatetrader.apigw.server.model;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * 网关请求的NETTY实现
 */
public class GatewayNettyRequest implements GatewayRequest {

    private final FullHttpRequest request;
    private final String method;
    private final String path;
    private final Map<String, List<String>> parameters;
    private final Map<String, GatewayCookie> cookies;
    private final ChannelHandlerContext context;

    public GatewayNettyRequest(FullHttpRequest request, ChannelHandlerContext context) {
        this.request = request;
        this.method = request.method().name();
        this.parameters = new HashMap<>();
        this.context = context;

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        this.path = queryStringDecoder.path();
        this.parameters.putAll(queryStringDecoder.parameters());

        // 提取cookie
        this.cookies = new HashMap<>();
        String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieHeader != null) {
            for (Cookie cookie : ServerCookieDecoder.LAX.decode(cookieHeader)) {
                // 同名cookie中后者会覆盖前者
                this.cookies.put(cookie.name(), new GatewayNettyCookie(cookie));
            }
        }
    }

    /**
     * 获取请求的发起方地址（网络连接的远端地址）
     *
     * @return 远端地址
     */
    @Override
    public String getRemoteAddr() {
        SocketAddress sa = context.channel().remoteAddress();
        if (sa instanceof InetSocketAddress) {
            return ((InetSocketAddress) sa).getAddress().getHostAddress();
        }
        return sa.toString();
    }

    /**
     * 获取此请求的请求路径（不包括查询）
     *
     * @return 请求路径
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * 获取请求使用的谓词
     *
     * @return 请求谓词
     */
    @Override
    public String getMethod() {
        return method;
    }

    /**
     * 获取指定的请求头
     *
     * @param headerName 请求头名称
     * @return 返回请求头的值
     */
    @Override
    public String getHeader(String headerName) {
        return request.headers().get(headerName);
    }

    /**
     * 获取请求中定义的所有参数名
     *
     * @return 参数名列表
     */
    @Override
    public Iterable<String> getParameterNames() {
        return parameters.keySet();
    }

    /**
     * 获取请求中的所有参数（包括URL查询和表单信息）
     *
     * @return 参数名 -> 参数值映射
     */
    @Override
    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    /**
     * 获取请求中的指定参数（包括URL中的查询和表单中的请求体信息）
     *
     * @param parameterName 参数名称
     * @return 返回参数值
     */
    @Override
    public String getParameter(String parameterName) {
        List<String> list = parameters.get(parameterName);
        return list != null ? list.get(0) : null;
    }

    /**
     * 覆盖或者增加新的请求参数
     * 用于需要修改请求参数的场景
     * <p>
     * 注意：此函数为线程不安全函数，线程安全问题由调用方保证
     *
     * @param parameterName  参数名
     * @param parameterValue 参数值
     */
    @Override
    public void setParameter(String parameterName, String parameterValue) {
        parameters.put(parameterName, Collections.singletonList(parameterValue));
    }

    /**
     * 将请求体中的参数以form方式展开到请求参数中
     */
    @Override
    public void extendFormToParameters() {
        HttpPostRequestDecoder requestDecoder = new HttpPostRequestDecoder(request);
        try {
            for (InterfaceHttpData data : requestDecoder.getBodyHttpDatas()) {
                // 我们只关心请求中包含的参数信息，而例如文件上传等其他信息会被丢弃
                if (data instanceof Attribute) {
                    Attribute attr = (Attribute) data;
                    try {
                        this.parameters
                            .computeIfAbsent(attr.getName(), key -> new ArrayList<>())
                            .add(attr.getValue());

                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        } finally {
            requestDecoder.destroy();
        }
    }

    /**
     * 获取请求中携带的cookie信息
     *
     * @return 所有cookie信息
     */
    @Override
    public Iterable<GatewayCookie> getCookies() {
        return cookies.values();
    }

    /**
     * 获取请求中指定的cookie的值
     *
     * @param cookieName cookie名称
     * @return 返回cookie的值
     */
    @Override
    public String getCookieValue(String cookieName) {
        GatewayCookie cookie = cookies.get(cookieName);
        return cookie != null ? cookie.value() : null;
    }

    /**
     * 获取请求体的输入流
     *
     * @return 输入流用于读取请求体
     */
    @Override
    public InputStream getInputStream() {
        return new ByteBufInputStream(request.content().duplicate().readerIndex(0));
    }
}
