package com.estatetrader.apigw.core.test.model;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.util.Lambda;
import org.apache.http.HttpHeaders;

import java.io.InputStream;
import java.util.*;

public class TestHttpRequest implements GatewayRequest {

    private final Map<String, String> params;
    private final Map<String, String> headers;
    private final String path;
    private final String method;

    public TestHttpRequest(Map<String, String> params) {
        this(params, Collections.emptyMap());
    }

    public TestHttpRequest(Map<String, String> params, Map<String, String> headers) {
        this.params = new HashMap<>();
        this.params.put("_aid", String.valueOf(1));
        this.params.putAll(params);

        this.headers = new HashMap<>();
        this.headers.put("host", "127.0.0.1");
        this.headers.put("x-forwarded-for", "127.0.0.1");
        this.headers.put(HttpHeaders.USER_AGENT, "client.tester");
        this.headers.putAll(headers);

        this.path = "test";
        this.method = "POST";
    }

    /**
     * 获取请求的发起方地址（网络连接的远端地址）
     *
     * @return 远端地址
     */
    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
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
        return headers.get(headerName);
    }

    /**
     * 获取请求中定义的所有参数名
     *
     * @return 参数名列表
     */
    @Override
    public Iterable<String> getParameterNames() {
        return params.keySet();
    }

    /**
     * 获取请求中的所有参数（包括URL查询和表单信息）
     *
     * @return 参数名 -> 参数值映射
     */
    @Override
    public Map<String, List<String>> getParameters() {
        return Lambda.mapValues(params, Collections::singletonList);
    }

    /**
     * 获取请求中的指定参数（包括URL中的查询和表单中的请求体信息）
     *
     * @param parameterName 参数名称
     * @return 返回参数值
     */
    @Override
    public String getParameter(String parameterName) {
        return params.get(parameterName);
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
        params.put(parameterName, parameterValue);
    }

    /**
     * 将请求体中的参数以form方式展开到请求参数中
     */
    @Override
    public void extendFormToParameters() {
        // nothing to do
    }

    /**
     * 获取请求中携带的cookie信息
     *
     * @return 所有cookie信息
     */
    @Override
    public Iterable<GatewayCookie> getCookies() {
        return Collections.emptyList();
    }

    /**
     * 获取请求中指定的cookie的值
     *
     * @param cookieName cookie名称
     * @return 返回cookie的值
     */
    @Override
    public String getCookieValue(String cookieName) {
        return null;
    }

    /**
     * 获取请求体的输入流
     *
     * @return 输入流用于读取请求体
     */
    @Override
    public InputStream getInputStream() {
        return null;
    }
}
