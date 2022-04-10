package com.estatetrader.apigw.request.handlers;

import com.alibaba.fastjson.JSON;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.request.RequestHandler;
import com.estatetrader.apigw.request.GatewayRequestHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequestHandler(handlerName = "status-request", urlPatterns = "/apigw/status")
public class StatusRequestHandler implements GatewayRequestHandler {
    /**
     * 处理请求
     *
     * @param request  请求
     * @param response 响应
     * @return 表示处理结束的future，返回null表示处理结果同步完成
     */
    @Override
    public CompletableFuture<Void> handle(GatewayRequest request, GatewayResponse response) throws IOException {
        Map<String, Object> status = new HashMap<>(2);
        status.put("finished", true);
        status.put("errors", Collections.emptyList());
        JSON.writeJSONString(response.getOutputStream(), status);
        response.setContentType("application/json; charset=utf-8");
        return null;
    }
}
