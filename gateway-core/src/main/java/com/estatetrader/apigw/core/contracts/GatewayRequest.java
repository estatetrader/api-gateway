package com.estatetrader.apigw.core.contracts;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 网关请求
 */
public interface GatewayRequest {

    /**
     * 获取请求的发起方地址（网络连接的远端地址）
     * @return 远端地址
     */
    String getRemoteAddr();

    /**
     * 获取此请求的请求路径（不包括查询）
     * @return 请求路径
     */
    String getPath();

    /**
     * 获取请求使用的谓词
     * @return 请求谓词
     */
    String getMethod();

    /**
     * 获取指定的请求头
     * @param headerName 请求头名称
     * @return 返回请求头的值
     */
    String getHeader(String headerName);

    /**
     * 获取客户端本地缓存拥有的ETag
     * @return 客户端ETag
     */
    default String getIfNoneMatch() {
        return getHeader("If-None-Match");
    }

    /**
     * 获取请求中定义的所有参数名
     * @return 参数名列表
     */
    Iterable<String> getParameterNames();

    /**
     * 获取请求中的所有参数（包括URL查询和表单信息）
     * @return 参数名 -> 参数值映射
     */
    Map<String, List<String>> getParameters();

    /**
     * 获取请求中的指定参数（包括URL中的查询和表单中的请求体信息）
     * @param parameterName 参数名称
     * @return 返回参数值
     */
    String getParameter(String parameterName);

    /**
     * 覆盖或者增加新的请求参数
     * 用于需要修改请求参数的场景
     *
     * 注意：此函数为线程不安全函数，线程安全问题由调用方保证
     *
     * @param parameterName 参数名
     * @param parameterValue 参数值
     */
    void setParameter(String parameterName, String parameterValue);

    /**
     * 将请求体中的参数以form方式展开到请求参数中
     */
    void extendFormToParameters();

    /**
     * 获取请求中携带的cookie信息
     * @return 所有cookie信息
     */
    Iterable<GatewayCookie> getCookies();

    /**
     * 获取请求中指定的cookie的值
     * @param cookieName cookie名称
     * @return 返回cookie的值
     */
    String getCookieValue(String cookieName);

    /**
     * 获取请求体的输入流
     * @return 输入流用于读取请求体
     */
    InputStream getInputStream();
}
