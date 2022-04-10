package com.estatetrader.apigw.request.handlers;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.request.RequestHandler;
import com.estatetrader.util.Md5Util;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.request.GatewayRequestHandler;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Extension(last = true) // static resource must be the last handler
@RequestHandler(handlerName = "static-resource-request",
    urlPatterns = {"/apigw/*.html", "/apigw/assets/*.*"},
    methods = "GET")
public class StaticResourceHandler implements GatewayRequestHandler {

    private final static String CONTEXT_PATH = "/apigw";

    private final ConcurrentHashMap<String, String> resourceETags = new ConcurrentHashMap<>();

    /**
     * 处理请求
     *
     * @param request  请求
     * @param response 响应
     * @return 表示处理结束的future，返回null表示处理结果同步完成
     */
    @Override
    public CompletableFuture<Void> handle(GatewayRequest request, GatewayResponse response) throws IOException {
        // 资源文件自启动后不会改变，我们设置一个小时的有效期
        response.setHeader("Cache-Control", "max-age=3600");

        String path = request.getPath();
        if (!path.startsWith(CONTEXT_PATH + "/")) {
            response.sendNotFound();
            return null;
        }

        String fileName = path.substring(CONTEXT_PATH.length() + 1);

        String etag = resourceETags.get(fileName);
        if (etag != null) { // 自启动后此资源的非第一次请求
            response.setETag(etag);
            if (etag.equals(request.getIfNoneMatch())) { // 资源未发生改变
                response.sendNotModified();
                return null; // 复用缓存
            }
        }

        InputStream stream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("static/" + fileName);

        if (stream == null) {
            response.sendNotFound();
            return null;
        }

        byte[] bytes;

        try {
            bytes = IOUtils.toByteArray(stream);
        } finally {
            stream.close();
        }

        if (etag == null) { // 自启动后此资源的第一次请求
            etag = Md5Util.computeToBase64(bytes);
            resourceETags.put(fileName, etag);
            response.setETag(etag);
        }

        if (etag.equals(request.getIfNoneMatch())) { // 资源未发生变化
            response.sendNotModified();
            return null; // 复用缓存
        }

        response.getOutputStream().write(bytes);

        return null;
    }
}
