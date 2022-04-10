package com.estatetrader.apigw.request.filters;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.request.GatewayRequestFilter;
import com.estatetrader.apigw.request.RequestFilter;

/**
 * 对于m.api post类型的请求，将请求体中的参数展开到请求参数中
 */
@RequestFilter(filterName = "FormParameterFilter", urlPatterns = "/apigw/m.api")
public class FormParameterFilter implements GatewayRequestFilter {
    /**
     * 执行过滤器，在实现方法中执行你的请求处理逻辑
     * 如果需要后续过滤器执行，请调用chain.next函数
     *
     * @param request  请求信息
     * @param response 请求的相应，用于接收处理结果
     * @param chain    过滤器链，用于方便执行后续过滤器
     */
    @Override
    public void filter(GatewayRequest request, GatewayResponse response, FilterChain chain) {
        // 针对网关的特殊场景，因为每个POST请求的参数都最终会被用到，
        // 为了简化多线程处理的问题，我们提前将请求体中的信息解析了出来
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            request.extendFormToParameters();
        }
        chain.next(request, response);
    }
}