package com.estatetrader.apigw.request.filters;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.request.GatewayRequestFilter;
import com.estatetrader.apigw.request.RequestFilter;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RequestFilter(filterName = "PathParamsFilter", urlPatterns = "/apigw/*.*/**")
public class PathParamsFilter implements GatewayRequestFilter {
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
        String[] parts = path.split("/");
        if (parts.length < 4) {
            chain.next(request, response);
            return;
        }
        String method = parts[2];
        request.setParameter("_mt", method);
        for (int i = 3; i < parts.length; i++) {
            String name = "$p" + (i - 3);
            String value;
            try {
                value = URLDecoder.decode(parts[i], StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            request.setParameter(name, value);
        }
        chain.next(request, response);
    }
}