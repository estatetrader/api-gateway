package com.estatetrader.apigw.core.phases.executing;

import com.estatetrader.algorithm.ObjectCache;
import com.estatetrader.algorithm.workflow.WorkflowDestination;
import com.estatetrader.algorithm.workflow.WorkflowGraph;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.phases.executing.access.CallFinished;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.dubboext.NotificationConsumerFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * 网关请求执行器，是网关核心代码的入口类
 */
@Service
public class RequestExecutor implements Closeable {

    @Bean
    public static ObjectCache<ByteArrayOutputStream> getBufferCache() {
        return new ObjectCache<>(200,
            () -> new ByteArrayOutputStream(4096),
            RequestExecutor::recycleStream
        );
    }

    private static ByteArrayOutputStream recycleStream(ByteArrayOutputStream stream) {
        // 为了保护内存，最多只缓存512K以下的stream
        if (stream.size() > 512 * 1024) {
            return null;
        } else {
            stream.reset();
            return stream;
        }
    }

    private final WorkflowExecution[] executeApiCall;

    private final WorkflowExecution startProcessing;

    private final WorkflowDestination finishProcessing;

    @SuppressWarnings("FieldMayBeFinal")
    @Value("${com.estatetrader.apigw.requestProcessingTimeout:60000}")
    private int requestProcessingTime = 60000;

    public RequestExecutor(RequestStarted.Execution requestStarted,
                           RequestFinished.Execution requestFinished,

                           CallStarted.Execution callStarted,
                           CallResultReceived.Execution callResultReceived,
                           CallFinished.Execution callFinished) {

        this.startProcessing = requestStarted;
        this.finishProcessing = requestFinished;

        this.executeApiCall = new WorkflowExecution[] {
            callStarted,
            callResultReceived,
            callFinished
        };

        // 为了防止NotificationConsumerFilter对本地notifications的干扰，我们需要关闭它
        // TODO 需要更加优雅的实现方式
        NotificationConsumerFilter.disabled = true;
    }

    /**
     * 执行web请求，作为网关请求处理的入口
     *
     * @param request 要处理的网关请求
     * @param response 处理结果的响应
     * @param apiSchema 涉及到的API定义
     * @return 返回的异步结果，请使用CompletableFuture.get()或CompletableFuture.then(xxx)等待处理结束
     */
    public CompletableFuture<Void> execute(GatewayRequest request, GatewayResponse response, ApiSchema apiSchema) {
        ApiContext context = new ApiContext(request, response, apiSchema, executeApiCall);
        WorkflowGraph graph = new WorkflowGraph(startProcessing, finishProcessing, context);
        return graph.start(requestProcessingTime);
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     */
    @Override
    public void close() {
        WorkflowGraph.shutdown();
    }
}
