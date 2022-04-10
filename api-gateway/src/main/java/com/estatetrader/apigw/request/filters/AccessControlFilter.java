package com.estatetrader.apigw.request.filters;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.features.SecurityFeature;
import com.estatetrader.apigw.request.GatewayRequestFilter;
import com.estatetrader.apigw.request.RequestFilter;
import org.springframework.beans.factory.annotation.Value;

@RequestFilter(filterName = "AccessControlFilter", urlPatterns = "/**")
public class AccessControlFilter implements GatewayRequestFilter {
    
    private static final   String               HEADER_ORGIN             = "Access-Control-Allow-Origin";
    private static final   String               HEADER_METHOD            = "Access-Control-Allow-Method";
    private static final   String               HEADER_CREDENTIALS       = "Access-Control-Allow-Credentials";
    private static final   String               HEADER_METHOD_VALUE      = "GET, POST, OPTIONS";
    private static final   String               HEADER_CREDENTIALS_VALUE = "true";
    private static final   String               TOKEN_CREDENTIAL = "Authorization, _tk";
    private static final   String               HEADER_ALLOW_HEADERS     = "Access-Control-Allow-Headers";
    private static final   String               HEADER_CACHE_CONTROL     = "Access-Control-Max-Age";
    private static final   String               CACHE_CONTROL_MAX_AGE    = "3600";

    private final SecurityFeature.Config securityConfig;

    @SuppressWarnings("FieldMayBeFinal")
    @Value("${gateway.origin-whitelist-enabled:true}")
    private boolean originWhitelistEnabled = true;

    public AccessControlFilter(SecurityFeature.Config securityConfig) {
        this.securityConfig = securityConfig;
    }

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
        String origin = request.getHeader("Origin");
        if (origin != null && (!originWhitelistEnabled || existInWhiteList(origin))) {
            response.setHeader(HEADER_ORGIN, origin);
            response.setHeader(HEADER_METHOD, HEADER_METHOD_VALUE);
            response.setHeader(HEADER_CREDENTIALS, HEADER_CREDENTIALS_VALUE);
            response.setHeader(HEADER_ALLOW_HEADERS, TOKEN_CREDENTIAL);
            response.setHeader(HEADER_CACHE_CONTROL, CACHE_CONTROL_MAX_AGE);
        }

        chain.next(request, response);
    }

    private boolean existInWhiteList(String origin){
        int index = origin.lastIndexOf('/');
        String originHost = origin.substring(index + 1);
        return securityConfig.getOriginWhiteList().containsKey(originHost);
    }
}
