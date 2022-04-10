package com.estatetrader.apigw.request;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;

/**
 * 网关请求过滤器，作为处理网关请求的核心方式
 */
public interface GatewayRequestFilter {

    /**
     * 执行过滤器，在实现方法中执行你的请求处理逻辑
     * 如果需要后续过滤器执行，请调用chain.next函数
     * @param request 请求信息
     * @param response 请求的相应，用于接收处理结果
     * @param chain 过滤器链，用于方便执行后续过滤器
     */
    void filter(GatewayRequest request,
                GatewayResponse response,
                FilterChain chain);

    interface FilterChain {
        void next(GatewayRequest request, GatewayResponse response);
    }
}
