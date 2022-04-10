package com.estatetrader.apigw.request.filters;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.request.GatewayRequestFilter;
import com.estatetrader.apigw.request.RequestFilter;

@RequestFilter(filterName = "ThirdPartyFilter", urlPatterns = "/apigw/*.*.api")
public class ThirdPartyFilter implements GatewayRequestFilter {
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
        String path = request.getPath();
        //第三方回调接口格式为'/#{apiName}.api'
        int index = path.lastIndexOf('/') + 1;
        String method = path.substring(index, path.length() - 4);
        request.setParameter("_mt", method);
        chain.next(request, response);
    }
}