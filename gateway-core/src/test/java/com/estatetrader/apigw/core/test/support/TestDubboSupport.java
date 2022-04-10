package com.estatetrader.apigw.core.test.support;

import com.estatetrader.dubboext.DubboExtProperty;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface TestDubboSupport {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    default <T> T asyncReturnDubbo(T result) {
        return asyncReturnDubbo(result, 0);
    }

    default <T> T asyncReturnDubbo(T result, long delay) {
        if (delay <= 0) {
            return result;
        }

        Map<String, String> notifications = DubboExtProperty.getCurrentNotifications();
        return asyncReturnDubbo(result, delay, notifications == null ? null : new HashMap<>(notifications));
    }

    default <T> T asyncReturnDubbo(T result, long delay, Map<String, String> notifications) {
        AppResponse response = new AppResponse();
        response.setValue(result);
        response.setAttachments(notifications);

        CompletableFuture<AppResponse> future = new CompletableFuture<>();
        RpcContext.getContext().setFuture(new FutureAdapter<T>(future));

        executor.schedule(() -> {
            future.complete(response);
        }, delay, TimeUnit.MILLISECONDS);
        return result instanceof Number ? result : null;
    }
}
