package com.estatetrader.apigw.core.phases.executing.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.estatetrader.algorithm.ObjectCache;
import com.estatetrader.algorithm.workflow.WorkflowDestination;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.core.GatewayException;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.util.RawString;
import com.google.common.base.Charsets;
import com.estatetrader.algorithm.workflow.DependentFailureException;
import com.estatetrader.algorithm.workflow.ResultAccessor;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.GatewayLogger;
import com.estatetrader.apigw.core.support.ApiMDCSupport;
import com.estatetrader.responseEntity.CallState;
import com.estatetrader.responseEntity.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 请求执行阶段
 *
 * 结束请求处理过程，包括整理各API调用结果，确定错误码，将结果写回响应等
 */
public interface RequestFinished {

    @Service
    class Execution implements WorkflowDestination, ApiMDCSupport {

        private static final byte[]  JSON_START           = "{\"stat\":".getBytes(Charsets.UTF_8);
        private static final byte[]  JSON_CONTENT         = ",\"content\":[".getBytes(Charsets.UTF_8);
        private static final byte[]  JSON_SPLIT           = ",".getBytes(Charsets.UTF_8);
        private static final byte[]  JSON_END             = "]}".getBytes(Charsets.UTF_8);

        private static final Logger logger = LoggerFactory.getLogger(RequestFinished.class);
        private static final Marker SERVLET_MARKER = MarkerFactory.getMarker("servlet");

        private static final List<ApiMethodCall> EMPTY_METHOD_CALL_ARRAY = Collections.emptyList();

        private final Extensions<CookieDispatcher> cookieDispatchers;

        private final Extensions<ResponseGenerator> responseGenerators;

        private final ObjectCache<ByteArrayOutputStream> bufferCache;

        private final GatewayLogger gatewayLogger;

        public Execution(ObjectCache<ByteArrayOutputStream> bufferCache,
                         Extensions<CookieDispatcher> cookieDispatchers,
                         Extensions<ResponseGenerator> responseGenerators,
                         GatewayLogger gatewayLogger) {

            this.bufferCache = bufferCache;
            this.cookieDispatchers = cookieDispatchers;
            this.responseGenerators = responseGenerators;
            this.gatewayLogger = gatewayLogger;
        }

        /**
         * finish the workflow
         *
         * @param originFailed the exception thrown by origin node, null if no error occurred
         * @param nodesFailed exceptions occurred of all failed nodes (excludes origin node)
         * @param result          the result accessor of the previous node of this node
         * @param context           workflow context
         */
        @Override
        public void finish(Throwable originFailed, List<Throwable> nodesFailed, ResultAccessor result, Object context) {
            ApiContext apiContext = (ApiContext) context;

            setupMDC(apiContext);
            try {
                AbstractReturnCode returnCode = processExceptions(originFailed, nodesFailed, apiContext);

                try {
                    for (CookieDispatcher p : cookieDispatchers) {
                        p.dispatch(apiContext);
                    }
                    apiContext.responseSize = writeProcessResult(apiContext, returnCode);
                } catch (Exception e) {
                    logger.error(SERVLET_MARKER, "output failed.", e);
                }

                apiContext.costTime = (int) (System.currentTimeMillis() - apiContext.startTime);
                gatewayLogger.logRequest(apiContext, returnCode);
            } finally {
                for (ApiMethodCall call : apiContext.apiCalls) {
                    if (call.buffer != null) {
                        bufferCache.release(call.buffer);
                        call.buffer = null;
                    }
                }

                cleanupMDC();
            }
        }

        private AbstractReturnCode processExceptions(Throwable originFailed,
                                                     List<Throwable> nodesFailed,
                                                     ApiContext apiContext) {

            Throwable finalOriginFailed;
            if (originFailed instanceof DependentFailureException && originFailed.getCause() != null) {
                finalOriginFailed = originFailed.getCause();
            } else {
                finalOriginFailed = originFailed;
            }

            if (finalOriginFailed instanceof GatewayException) {
                GatewayException ge = (GatewayException) finalOriginFailed;
                printGatewayException(ge);
                return ge.getCode();
            }

            if (originFailed != null) {
                logger.error("init request failed.", originFailed);
                return ApiReturnCode.FATAL_ERROR;
            }

            if (!nodesFailed.isEmpty()) {
                for (Throwable throwable : nodesFailed) {
                    logger.error("execute node failed.", throwable);
                }
                return ApiReturnCode.FATAL_ERROR;
            }

            if (apiContext.requestErrorCode != null) {
                return apiContext.requestErrorCode;
            }

            return ApiReturnCode.SUCCESS;
        }

        private void printGatewayException(GatewayException e) {
            if (e.getCause() != null && logger.isErrorEnabled()) {
                logger.error("gateway exception: " + e.getMessage(), e);
            }
        }

        private int writeProcessResult(ApiContext apiContext,
                                       AbstractReturnCode returnCode) throws IOException {
            if (returnCode == ApiReturnCode.FATAL_ERROR) {
                apiContext.response.sendError(returnCode.toString());
                return 0;
            }

            return output(apiContext, returnCode);
        }

        /**
         * 输出返回到调用端
         */
        private int output(ApiContext apiContext, AbstractReturnCode code) throws IOException {
            if (apiContext.apiCalls.size() == 1 &&
                apiContext.apiCalls.get(0).method.returnType.equals(RawString.class)) {
                return outputRawString(code, apiContext.apiCalls.get(0), apiContext.response);
            }

            Response apiResponse = new Response();

            // 如果request处理失败，则实际上所有的API都没有处理，因此不输出它们的返回值
            List<ApiMethodCall> callsToOutput = code == ApiReturnCode.SUCCESS ?
                apiContext.apiCalls : EMPTY_METHOD_CALL_ARRAY;

            for (ResponseGenerator g : responseGenerators) {
                g.process(apiContext, code, callsToOutput, apiResponse);
            }

            return writeToResponse(callsToOutput, apiContext.response, apiResponse);
        }

        private int writeToResponse(List<ApiMethodCall> calls,
                                    GatewayResponse response,
                                    Response apiResponse) throws IOException {

            int len = 0;
            OutputStream output = response.getOutputStream();

            output.write(JSON_START);
            len += JSON_START.length;

            byte[] serializedApiResponse = serializeApiResponse(apiResponse);
            output.write(serializedApiResponse);
            len += serializedApiResponse.length;

            output.write(JSON_CONTENT);
            len += JSON_CONTENT.length;

            boolean first = true;
            for (ApiMethodCall call : calls) {
                if (first) {
                    first = false;
                } else {
                    output.write(JSON_SPLIT);
                    len += JSON_SPLIT.length;
                }

                call.buffer.writeTo(output);
                len += call.buffer.size();
            }

            output.write(JSON_END);
            len += JSON_END.length;

            return len;
        }

        private byte[] serializeApiResponse(Response apiResponse) {
            return JSON.toJSONBytes(
                apiResponse,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullListAsEmpty
            );
        }

        private int outputRawString(AbstractReturnCode code,
                                     ApiMethodCall call,
                                     GatewayResponse response) throws IOException {

            // rawString的处理，将dubbo service返回的结果直接输出
            OutputStream output = response.getOutputStream();
            if (code == ApiReturnCode.SUCCESS && call.success()) {
                call.buffer.writeTo(output);
                return call.buffer.size();
            }

            String errorMessage;
            if (code != ApiReturnCode.SUCCESS) {
                errorMessage = code.getName();
            } else {
                errorMessage = call.getReturnMessage();
            }
            response.sendError(errorMessage);
            return 0;
        }
    }

    /**
     * 在此实现类中将需要cookie种给客户端
     */
    interface CookieDispatcher {
        void dispatch(ApiContext context);
    }

    /**
     * 在此实现类中将需要的信息写入通用返回结构
     */
    interface ResponseGenerator {
        void process(ApiContext context, AbstractReturnCode code, List<ApiMethodCall> calls, Response response);
    }

    @Extension(first = true)
    class ResponseGeneratorImpl implements ResponseGenerator {

        private final boolean exposeErrorDetail;

        public ResponseGeneratorImpl(@Value("${gateway.expose-error-detail:false}") boolean exposeErrorDetail) {
            this.exposeErrorDetail = exposeErrorDetail;
        }

        @Override
        public void process(ApiContext context,
                            AbstractReturnCode code,
                            List<ApiMethodCall> calls,
                            Response response) {

            response.code = code.getDisplay().getCode();
            response.stateList = new ArrayList<>(calls.size());

            if (context.cid != null) {
                response.cid = context.cid;
            }

            for (ApiMethodCall call : calls) {
                CallState state = new CallState();
                state.code = call.getReturnCode();
                state.msg = call.getReturnMessage();
                if (exposeErrorDetail && call.getReturnCode() != call.getOriginCode()) {
                    // debug模式将实际error code外露到msg中
                    state.msg = call.getOriginMessage() + ":" + call.getOriginCode();
                }
                state.length = call.resultLen;
                response.stateList.add(state);
            }

            response.systime = System.currentTimeMillis();
        }
    }
}
