package com.estatetrader.define;

/**
 * 用于拦截和修改API返回值的过滤器
 */
public interface ResponseFilter {

    /**
     * 将在被拦截的API返回后执行，在该函数内部提供
     * @param response      被拦截的接口的返回值
     */
    void filter(Object response);
}
