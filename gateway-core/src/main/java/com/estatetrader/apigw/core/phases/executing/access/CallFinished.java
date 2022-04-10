package com.estatetrader.apigw.core.phases.executing.access;

import com.estatetrader.algorithm.ObjectCache;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.GatewayLogger;
import com.estatetrader.apigw.core.support.ApiMDCSupport;
import com.estatetrader.apigw.core.phases.executing.serialize.ResponseSerializer;
import com.estatetrader.core.GatewayException;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.util.RawString;
import com.google.common.base.Charsets;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * API执行阶段
 *
 * API执行结束，包括分析错误码、对结果进行序列化等
 */
public interface CallFinished {

    @Service
    class Execution implements WorkflowExecution.Sync.Resource, ApiMDCSupport {

        private static final byte[]  JSON_EMPTY           = "{}".getBytes(Charsets.UTF_8);

        private final Extensions<CallExceptionHandler> callExceptionHandlers;
        private final ResponseSerializer responseSerializer;
        private final ObjectCache<ByteArrayOutputStream> bufferCache;
        private final GatewayLogger gatewayLogger;

        public Execution(ObjectCache<ByteArrayOutputStream> bufferCache,
                         Extensions<CallExceptionHandler> callExceptionHandlers,
                         Extensions<ResponseSerializer> responseSerializer,
                         GatewayLogger gatewayLogger) {
            this.bufferCache = bufferCache;
            this.callExceptionHandlers = callExceptionHandlers;
            this.responseSerializer = responseSerializer.singleton();
            this.gatewayLogger = gatewayLogger;
        }

        @Override
        public boolean acceptPreviousFailure() {
            return true;
        }

        /**
         * before the body of the execution of the node
         *
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         */
        @Override
        public void setup(WorkflowPipeline pipeline) {
            setupMDC((ApiContext) pipeline.getContext(), ((ApiMethodCall) pipeline.getParam()).method);
        }

        /**
         * the body the execution of the node
         *
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         */
        @Override
        public void body(WorkflowPipeline pipeline) throws GatewayException {
            ApiContext apiContext = (ApiContext) pipeline.getContext();
            ApiMethodCall call = (ApiMethodCall) pipeline.getParam();
            Throwable throwable = pipeline.hasPreviousFailed();
            if (throwable != null) {
                // 处理异常
                callExceptionHandlers.chain(CallExceptionHandler::handle, apiContext, call, throwable).go();
            }

            call.buffer = serializeResult(call, apiContext);
            call.costTime = (int) (System.currentTimeMillis() - call.startTime);
            call.resultLen = call.buffer.size();

            if (call.method.recordResult) {
                try {
                    call.serializedResult = call.buffer.toString(Charsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }

            if (!call.fromClient) {
                // if the call is not from client, it is the request-processor's responsibility
                // to return the buffer to buffer cache
                bufferCache.release(call.buffer);
                call.buffer = null;
            }

            gatewayLogger.logAccess(apiContext, call);
        }

        private ByteArrayOutputStream serializeResult(ApiMethodCall call, ApiContext context)
            throws GatewayException {

            ByteArrayOutputStream buffer = bufferCache.acquire();
            Object result = getResultForSerialize(call);
            try {
                if (result == null) {
                    if (!call.method.returnType.equals(RawString.class)) {
                        buffer.write(JSON_EMPTY);
                    }
                } else if (result instanceof RawString) {
                    RawString rs = (RawString) result;
                    if (rs.value != null) {
                        buffer.write(rs.value.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    responseSerializer.toJson(buffer, result, call, context);
                }

                // 一切正常，主调函数应在恰当的时机通过 bufferCache.release 回收此buffer，以减少内存压力
                return buffer;
            } catch (IOException e) {
                // 在出现异常时回收刚刚申请的buffer，因此此时主调函数已经无法拿到这个buffer
                bufferCache.release(buffer);
                throw new GatewayException(ApiReturnCode.SERIALIZE_FAILED, e);
            }
        }

        private Object getResultForSerialize(ApiMethodCall call) {
            if (call.method.authenticationMethod) {
                return null;
            }
            if (call.success()) {
                if (call.result instanceof RawString) {
                    return call.result;
                }
                return call.method.responseWrapper.wrap(call.result);
            }
            return null;
        }

        /**
         * after the body the execution of the node, will be executed whether the run success or fail
         *
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         */
        @Override
        public void cleanup(WorkflowPipeline pipeline) {
            cleanupMDC();
        }
    }

    /**
     * 用于处理API调用产生的异常
     */
    interface CallExceptionHandler {
        void handle(ApiContext context, ApiMethodCall call, Throwable throwable, Next.NoResult<RuntimeException> next);
    }

    @Extension(last = true)
    class DefaultCallExceptionHandler implements CallExceptionHandler {
        @Override
        public void handle(ApiContext context,
                           ApiMethodCall call,
                           Throwable throwable,
                           Next.NoResult<RuntimeException> next) {
            call.setCode(ApiReturnCode.UNKNOWN_ERROR);
        }
    }
}
