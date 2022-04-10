package com.estatetrader.apigw.request.handlers;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.core.phases.executing.RequestExecutor;
import com.estatetrader.apigw.request.RequestHandler;
import com.estatetrader.apigw.request.GatewayRequestHandler;

import java.util.concurrent.CompletableFuture;

/**
 * 处理API调用的请求
 */
@RequestHandler(handlerName = "api-request", urlPatterns = {"/apigw/m.api", "/apigw/*.*.api"})
public class ApiRequestHandler implements GatewayRequestHandler {

    private final RequestExecutor executor;
    private final ApiSchema apiSchema;

    public ApiRequestHandler(RequestExecutor executor, ApiSchema apiSchema) {
        this.executor = executor;
        this.apiSchema = apiSchema;
    }

    /**
     * 处理请求
     *
     * @param request  请求
     * @param response 响应
     * @return 表示处理结束的future，返回null表示处理结果同步完成
     */
    @Override
    public CompletableFuture<Void> handle(GatewayRequest request, GatewayResponse response) {
        return executor.execute(request, response, apiSchema);
    }
}
