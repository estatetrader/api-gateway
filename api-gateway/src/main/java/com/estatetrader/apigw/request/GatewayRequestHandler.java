package com.estatetrader.apigw.request;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 网关请求的处理器，每个请求最多只有一个处理器可以处理
 */
public interface GatewayRequestHandler {
    /**
     * 处理请求
     * @param request 请求
     * @param response 响应
     * @return 表示处理结束的future，返回null表示处理结果同步完成
     */
    CompletableFuture<Void> handle(GatewayRequest request, GatewayResponse response) throws IOException;
}
